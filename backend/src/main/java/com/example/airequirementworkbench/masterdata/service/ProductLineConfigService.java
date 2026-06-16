package com.example.airequirementworkbench.masterdata.service;

import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.feature.repository.FeatureNodeRepository;
import com.example.airequirementworkbench.masterdata.dto.ProductLineConfigDtos.ProductLineDetailDto;
import com.example.airequirementworkbench.masterdata.dto.ProductLineConfigDtos.SaveProductLineRequest;
import com.example.airequirementworkbench.masterdata.entity.ProductLine;
import com.example.airequirementworkbench.masterdata.repository.ProductLineRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductLineConfigService {
  private static final Set<String> PRODUCT_TYPES = Set.of("face_to_customer", "internal", "public_service", "design_spec");
  private static final Set<String> PLATFORMS = Set.of(
      "yunlian_front",
      "yunzu_front",
      "yunzu_app",
      "middle_platform",
      "yunlian_back",
      "yunzu_back",
      "lianxin",
      "lianxin_app");

  private final ProductLineRepository productLineRepository;
  private final FeatureNodeRepository featureNodeRepository;
  private final IdGenerator idGenerator;
  private final Long mockUserId;

  public ProductLineConfigService(
      ProductLineRepository productLineRepository,
      FeatureNodeRepository featureNodeRepository,
      IdGenerator idGenerator,
      @Value("${app.mock-user-id:1}") Long mockUserId) {
    this.productLineRepository = productLineRepository;
    this.featureNodeRepository = featureNodeRepository;
    this.idGenerator = idGenerator;
    this.mockUserId = mockUserId;
  }

  @Transactional(readOnly = true)
  public List<ProductLineDetailDto> list(String keyword) {
    String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
    return productLineRepository.findByDeletedFalseOrderByUpdatedAtDesc().stream()
        .filter(line -> matchesKeyword(line, normalizedKeyword))
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProductLineDetailDto detail(Long id) {
    return toDto(findActive(id));
  }

  @Transactional
  public ProductLineDetailDto create(SaveProductLineRequest request) {
    NormalizedPayload payload = normalize(request);
    assertNameAvailable(payload.lineName(), null);
    assertCodeAvailable(payload.lineCode(), null);

    ProductLine line = new ProductLine();
    line.setId(idGenerator.nextId());
    line.setLineCode(payload.lineCode());
    line.setLineName(payload.lineName());
    line.setOwners(payload.owners());
    line.setProductType(payload.productType());
    line.setPlatforms(payload.platforms());
    line.setDescription(payload.description());
    line.setVersion(0);
    line.setIsProcessing(false);
    line.setStatus("enabled");
    line.setCreatedBy(mockUserId);
    line.setUpdatedBy(mockUserId);
    line.setDeleted(false);
    productLineRepository.save(line);
    return toDto(line);
  }

  @Transactional
  public ProductLineDetailDto update(Long id, SaveProductLineRequest request) {
    ProductLine line = findActive(id);
    NormalizedPayload payload = normalize(request);
    assertNameAvailable(payload.lineName(), id);
    assertCodeAvailable(payload.lineCode(), id);

    line.setLineCode(payload.lineCode());
    line.setLineName(payload.lineName());
    line.setOwners(payload.owners());
    line.setProductType(payload.productType());
    line.setPlatforms(payload.platforms());
    line.setDescription(payload.description());
    line.setUpdatedBy(mockUserId);
    productLineRepository.save(line);
    return toDto(line);
  }

  @Transactional
  public void delete(Long id) {
    ProductLine line = findActive(id);
    line.setDeleted(true);
    line.setStatus("disabled");
    line.setUpdatedBy(mockUserId);
    line.setUpdatedAt(LocalDateTime.now());
    productLineRepository.save(line);
  }

  private ProductLine findActive(Long id) {
    ProductLine line = productLineRepository.findById(id)
        .orElseThrow(() -> new BusinessException("PRODUCT_LINE_NOT_FOUND", "产品线不存在"));
    if (Boolean.TRUE.equals(line.getDeleted())) {
      throw new BusinessException("PRODUCT_LINE_DELETED", "产品线已删除");
    }
    return line;
  }

  private NormalizedPayload normalize(SaveProductLineRequest request) {
    String lineName = normalizeRequired(request.lineName(), "产品线名称不能为空");
    String lineCode = normalizeOptional(request.lineCode());
    List<String> owners = normalizeList(request.owners(), "负责人不能为空");
    String productType = normalizeRequired(request.productType(), "产品线类型不能为空");
    if (!PRODUCT_TYPES.contains(productType)) {
      throw new BusinessException("INVALID_PRODUCT_LINE_TYPE", "产品线类型不支持");
    }
    List<String> platforms = normalizeList(request.platforms(), "涉及平台不能为空");
    for (String platform : platforms) {
      if (!PLATFORMS.contains(platform)) {
        throw new BusinessException("INVALID_PRODUCT_LINE_PLATFORM", "涉及平台不支持：" + platform);
      }
    }
    return new NormalizedPayload(lineCode, lineName, owners, productType, platforms, normalizeOptional(request.description()));
  }

  private List<String> normalizeList(Collection<String> values, String emptyMessage) {
    if (values == null) {
      throw new BusinessException("VALIDATION_ERROR", emptyMessage);
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        normalized.add(value.trim());
      }
    }
    if (normalized.isEmpty()) {
      throw new BusinessException("VALIDATION_ERROR", emptyMessage);
    }
    return List.copyOf(normalized);
  }

  private String normalizeRequired(String value, String emptyMessage) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException("VALIDATION_ERROR", emptyMessage);
    }
    return value.trim();
  }

  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private void assertNameAvailable(String lineName, Long excludeId) {
    if (productLineRepository.existsActiveName(lineName, excludeId)) {
      throw new BusinessException("PRODUCT_LINE_NAME_DUPLICATED", "产品线名称已存在");
    }
  }

  private void assertCodeAvailable(String lineCode, Long excludeId) {
    if (lineCode != null && productLineRepository.existsActiveCode(lineCode, excludeId)) {
      throw new BusinessException("PRODUCT_LINE_CODE_DUPLICATED", "产品线编码已存在");
    }
  }

  private boolean matchesKeyword(ProductLine line, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    boolean matchedByNameOrCode = line.getLineName().toLowerCase(Locale.ROOT).contains(keyword)
        || (line.getLineCode() != null && line.getLineCode().toLowerCase(Locale.ROOT).contains(keyword));
    if (matchedByNameOrCode) {
      return true;
    }
    return safeList(line.getOwners()).stream().anyMatch(owner -> owner.toLowerCase(Locale.ROOT).contains(keyword));
  }

  private ProductLineDetailDto toDto(ProductLine line) {
    return new ProductLineDetailDto(
        line.getId(),
        line.getLineCode(),
        line.getLineName(),
        safeList(line.getOwners()),
        line.getProductType(),
        safeList(line.getPlatforms()),
        line.getDescription(),
        line.getVersion(),
        line.getIsProcessing(),
        featureNodeRepository.countByProductLineIdAndDeletedFalse(line.getId()),
        true,
        true,
        line.getCreatedAt(),
        line.getUpdatedAt());
  }

  private List<String> safeList(List<String> values) {
    return values == null ? List.of() : values;
  }

  private record NormalizedPayload(
      String lineCode,
      String lineName,
      List<String> owners,
      String productType,
      List<String> platforms,
      String description) {}
}
