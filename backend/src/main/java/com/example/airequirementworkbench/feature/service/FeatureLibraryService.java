package com.example.airequirementworkbench.feature.service;

import com.example.airequirementworkbench.common.IdGenerator;
import com.example.airequirementworkbench.common.exception.BusinessException;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.CreateNodeRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureContentBlockDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureHistoryDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.FeatureNodeDto;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.MoveNodeRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.SaveContentBlockRequest;
import com.example.airequirementworkbench.feature.dto.FeatureLibraryDtos.UpdateNodeRequest;
import com.example.airequirementworkbench.feature.entity.FeatureContentBlock;
import com.example.airequirementworkbench.feature.entity.FeatureHistory;
import com.example.airequirementworkbench.feature.entity.FeatureNode;
import com.example.airequirementworkbench.feature.repository.FeatureContentBlockRepository;
import com.example.airequirementworkbench.feature.repository.FeatureHistoryRepository;
import com.example.airequirementworkbench.feature.repository.FeatureNodeRepository;
import com.example.airequirementworkbench.masterdata.entity.ProductLine;
import com.example.airequirementworkbench.masterdata.repository.ProductLineRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FeatureLibraryService {
  private static final Set<String> NODE_TYPES = Set.of("module", "feature");
  private static final Set<String> BLOCK_TYPES = Set.of("overview", "rule", "field", "api", "screenshot");

  private final FeatureNodeRepository nodeRepository;
  private final FeatureContentBlockRepository blockRepository;
  private final FeatureHistoryRepository historyRepository;
  private final ProductLineRepository productLineRepository;
  private final IdGenerator idGenerator;
  private final Long mockUserId;

  public FeatureLibraryService(
      FeatureNodeRepository nodeRepository,
      FeatureContentBlockRepository blockRepository,
      FeatureHistoryRepository historyRepository,
      ProductLineRepository productLineRepository,
      IdGenerator idGenerator,
      @Value("${app.mock-user-id:1}") Long mockUserId) {
    this.nodeRepository = nodeRepository;
    this.blockRepository = blockRepository;
    this.historyRepository = historyRepository;
    this.productLineRepository = productLineRepository;
    this.idGenerator = idGenerator;
    this.mockUserId = mockUserId;
  }

  @Transactional(readOnly = true)
  public List<FeatureNodeDto> tree(Long productLineId, String keyword) {
    ensureProductLine(productLineId);
    List<FeatureNode> nodes = nodeRepository.findByProductLineIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(productLineId);
    // 关键字查询仍保留命中节点的祖先链，否则前端无法在树上看出命中节点属于哪个模块。
    List<FeatureNode> visibleNodes = applyKeyword(nodes, keyword);
    if (visibleNodes.isEmpty()) {
      return List.of();
    }
    Map<Long, List<FeatureContentBlockDto>> blocksByFeature = blockRepository
        .findByFeatureIdInAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(visibleNodes.stream().map(FeatureNode::getId).toList())
        .stream()
        .map(this::toBlockDto)
        .collect(Collectors.groupingBy(FeatureContentBlockDto::featureId, LinkedHashMap::new, Collectors.toList()));

    Map<Long, MutableNode> dtoMap = new LinkedHashMap<>();
    visibleNodes.stream()
        .sorted(nodeComparator())
        .forEach(node -> dtoMap.put(node.getId(), new MutableNode(node, blocksByFeature.getOrDefault(node.getId(), List.of()))));

    List<MutableNode> roots = new ArrayList<>();
    for (MutableNode mutable : dtoMap.values()) {
      Long parentId = mutable.node.getParentId();
      MutableNode parent = parentId == null ? null : dtoMap.get(parentId);
      if (parent == null) {
        roots.add(mutable);
      } else {
        parent.children.add(mutable);
      }
    }
    sortMutable(roots);
    return roots.stream().map(MutableNode::toDto).toList();
  }

  @Transactional
  public FeatureNodeDto createNode(CreateNodeRequest request) {
    ensureProductLine(request.productLineId());
    validateNodeType(request.nodeType());
    validateParent(request.productLineId(), request.parentId());
    assertSiblingNameAvailable(request.productLineId(), request.parentId(), request.name(), null);

    FeatureNode node = new FeatureNode();
    node.setId(idGenerator.nextId());
    node.setProductLineId(request.productLineId());
    node.setParentId(request.parentId());
    node.setName(request.name().trim());
    node.setDescription(normalizeText(request.description()));
    node.setNodeType(request.nodeType());
    node.setStatus("added");
    node.setSortOrder(nodeRepository.maxSortOrder(request.productLineId(), request.parentId()) + 1);
    nodeRepository.save(node);
    writeHistory(node.getId(), "added", "新增功能节点：" + node.getName());
    return toNodeDto(node, List.of(), List.of());
  }

  @Transactional
  public FeatureNodeDto updateNode(Long id, UpdateNodeRequest request) {
    FeatureNode node = findActiveNode(id);
    boolean changed = false;
    if (request.getNodeType() != null) {
      if (!StringUtils.hasText(request.getNodeType())) {
        throw new BusinessException("VALIDATION_ERROR", "节点类型不能为空");
      }
      validateNodeType(request.getNodeType());
      if (!Objects.equals(node.getNodeType(), request.getNodeType())) {
        node.setNodeType(request.getNodeType());
        changed = true;
      }
    }
    if (request.getName() != null && !StringUtils.hasText(request.getName())) {
      throw new BusinessException("VALIDATION_ERROR", "名称不能为空");
    }
    if (request.getName() != null && !Objects.equals(node.getName(), request.getName().trim())) {
      Long targetParentId = request.isParentIdProvided() ? request.getParentId() : node.getParentId();
      assertSiblingNameAvailable(node.getProductLineId(), targetParentId, request.getName(), node.getId());
      node.setName(request.getName().trim());
      changed = true;
    }
    if (request.getDescription() != null && !Objects.equals(node.getDescription(), normalizeText(request.getDescription()))) {
      node.setDescription(normalizeText(request.getDescription()));
      changed = true;
    }
    if (request.isParentIdProvided() && !Objects.equals(node.getParentId(), request.getParentId())) {
      moveNodeInternal(node, request.getParentId(), nodeRepository.maxSortOrder(node.getProductLineId(), request.getParentId()) + 1);
      writeHistory(node.getId(), "moved", "移动功能节点到新的父节点");
      changed = true;
    }
    if (changed) {
      node.setStatus("modified");
      nodeRepository.save(node);
      writeHistory(node.getId(), "modified", "编辑功能节点：" + node.getName());
    }
    return toNodeDto(node, List.of(), blockRepository.findByFeatureIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(node.getId()).stream().map(this::toBlockDto).toList());
  }

  @Transactional
  public FeatureNodeDto moveNode(Long id, MoveNodeRequest request) {
    FeatureNode node = findActiveNode(id);
    moveNodeInternal(node, request.parentId(), request.index());
    node.setStatus("modified");
    nodeRepository.save(node);
    writeHistory(node.getId(), "moved", "移动功能节点排序或父节点");
    return toNodeDto(node, List.of(), blockRepository.findByFeatureIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(node.getId()).stream().map(this::toBlockDto).toList());
  }

  @Transactional
  public void deleteNode(Long id) {
    FeatureNode root = findActiveNode(id);
    List<FeatureNode> allNodes = nodeRepository.findByProductLineIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(root.getProductLineId());
    Map<Long, List<FeatureNode>> childrenByParent = childrenByParent(allNodes);
    // 功能树删除采用级联软删除，保留历史和数据追溯，不做物理删除。
    List<FeatureNode> toDelete = collectDescendants(root, childrenByParent);
    toDelete.forEach(node -> {
      node.setDeleted(true);
      node.setStatus("deleted");
      writeHistory(node.getId(), "deleted", Objects.equals(node.getId(), root.getId()) ? "删除功能节点：" + node.getName() : "随父节点删除：" + node.getName());
    });
    nodeRepository.saveAll(toDelete);
    reorderSiblings(root.getProductLineId(), root.getParentId());
  }

  @Transactional(readOnly = true)
  public List<FeatureHistoryDto> history(Long nodeId) {
    findActiveNode(nodeId);
    return historyRepository.findByFeatureIdOrderByCreatedAtDesc(nodeId).stream()
        .map(history -> new FeatureHistoryDto(
            history.getId(),
            history.getFeatureId(),
            history.getOperationType(),
            history.getDescription(),
            history.getOperatorId(),
            history.getCreatedAt()))
        .toList();
  }

  @Transactional
  public FeatureContentBlockDto createContentBlock(Long featureId, SaveContentBlockRequest request) {
    findActiveNode(featureId);
    validateBlockType(request.blockType());
    FeatureContentBlock block = new FeatureContentBlock();
    block.setId(idGenerator.nextId());
    block.setFeatureId(featureId);
    applyBlockFields(block, request);
    block.setSortOrder(request.sortOrder() == null ? blockRepository.maxSortOrder(featureId) + 1 : request.sortOrder());
    blockRepository.save(block);
    return toBlockDto(block);
  }

  @Transactional
  public FeatureContentBlockDto updateContentBlock(Long blockId, SaveContentBlockRequest request) {
    FeatureContentBlock block = findActiveBlock(blockId);
    validateBlockType(request.blockType());
    applyBlockFields(block, request);
    if (request.sortOrder() != null) {
      block.setSortOrder(request.sortOrder());
    }
    blockRepository.save(block);
    return toBlockDto(block);
  }

  @Transactional
  public void deleteContentBlock(Long blockId) {
    FeatureContentBlock block = findActiveBlock(blockId);
    block.setDeleted(true);
    blockRepository.save(block);
  }

  private void moveNodeInternal(FeatureNode node, Long targetParentId, int targetIndex) {
    validateParent(node.getProductLineId(), targetParentId);
    // 移动节点时必须由后端兜底防环，不能只依赖前端下拉选项过滤。
    ensureNotSelfOrDescendant(node, targetParentId);
    assertSiblingNameAvailable(node.getProductLineId(), targetParentId, node.getName(), node.getId());
    Long oldParentId = node.getParentId();
    List<FeatureNode> targetSiblings = nodeRepository.findSiblings(node.getProductLineId(), targetParentId).stream()
        .filter(sibling -> !Objects.equals(sibling.getId(), node.getId()))
        .collect(Collectors.toCollection(ArrayList::new));
    int clampedIndex = Math.max(0, Math.min(targetIndex, targetSiblings.size()));
    targetSiblings.add(clampedIndex, node);
    node.setParentId(targetParentId);
    applySiblingOrder(targetSiblings);
    nodeRepository.saveAll(targetSiblings);
    if (!Objects.equals(oldParentId, targetParentId)) {
      reorderSiblings(node.getProductLineId(), oldParentId);
    }
  }

  private void ensureProductLine(Long productLineId) {
    ProductLine productLine = productLineRepository.findById(productLineId)
        .orElseThrow(() -> new BusinessException("PRODUCT_LINE_NOT_FOUND", "产品线不存在"));
    if (Boolean.TRUE.equals(productLine.getDeleted()) || !"enabled".equals(productLine.getStatus())) {
      throw new BusinessException("PRODUCT_LINE_DISABLED", "产品线不可用");
    }
  }

  private FeatureNode validateParent(Long productLineId, Long parentId) {
    if (parentId == null) {
      return null;
    }
    FeatureNode parent = findActiveNode(parentId);
    if (!Objects.equals(parent.getProductLineId(), productLineId)) {
      throw new BusinessException("INVALID_PARENT", "父节点必须属于同一产品线");
    }
    return parent;
  }

  private FeatureNode findActiveNode(Long id) {
    FeatureNode node = nodeRepository.findById(id)
        .orElseThrow(() -> new BusinessException("FEATURE_NODE_NOT_FOUND", "功能节点不存在"));
    if (Boolean.TRUE.equals(node.getDeleted())) {
      throw new BusinessException("FEATURE_NODE_DELETED", "功能节点已删除");
    }
    return node;
  }

  private FeatureContentBlock findActiveBlock(Long id) {
    FeatureContentBlock block = blockRepository.findById(id)
        .orElseThrow(() -> new BusinessException("CONTENT_BLOCK_NOT_FOUND", "内容块不存在"));
    if (Boolean.TRUE.equals(block.getDeleted())) {
      throw new BusinessException("CONTENT_BLOCK_DELETED", "内容块已删除");
    }
    return block;
  }

  private void ensureNotSelfOrDescendant(FeatureNode node, Long targetParentId) {
    if (targetParentId == null) {
      return;
    }
    if (Objects.equals(node.getId(), targetParentId)) {
      throw new BusinessException("INVALID_MOVE", "不能移动到自己下面");
    }
    List<FeatureNode> allNodes = nodeRepository.findByProductLineIdAndDeletedFalseOrderBySortOrderAscCreatedAtAsc(node.getProductLineId());
    Map<Long, List<FeatureNode>> childrenByParent = childrenByParent(allNodes);
    ArrayDeque<FeatureNode> queue = new ArrayDeque<>(childrenByParent.getOrDefault(node.getId(), List.of()));
    while (!queue.isEmpty()) {
      FeatureNode child = queue.removeFirst();
      if (Objects.equals(child.getId(), targetParentId)) {
        throw new BusinessException("INVALID_MOVE", "不能移动到自己的子孙节点下面");
      }
      queue.addAll(childrenByParent.getOrDefault(child.getId(), List.of()));
    }
  }

  private void assertSiblingNameAvailable(Long productLineId, Long parentId, String name, Long excludeId) {
    if (!StringUtils.hasText(name)) {
      throw new BusinessException("VALIDATION_ERROR", "名称不能为空");
    }
    if (nodeRepository.existsSiblingName(productLineId, parentId, name.trim(), excludeId)) {
      throw new BusinessException("FEATURE_NAME_DUPLICATED", "同级功能节点名称不能重复");
    }
  }

  private List<FeatureNode> applyKeyword(List<FeatureNode> nodes, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return nodes;
    }
    String normalized = keyword.trim().toLowerCase(Locale.ROOT);
    Map<Long, FeatureNode> byId = nodes.stream().collect(Collectors.toMap(FeatureNode::getId, node -> node));
    Set<Long> included = new HashSet<>();
    for (FeatureNode node : nodes) {
      String name = node.getName() == null ? "" : node.getName().toLowerCase(Locale.ROOT);
      String description = node.getDescription() == null ? "" : node.getDescription().toLowerCase(Locale.ROOT);
      if (name.contains(normalized) || description.contains(normalized)) {
        FeatureNode cursor = node;
        while (cursor != null && included.add(cursor.getId())) {
          cursor = cursor.getParentId() == null ? null : byId.get(cursor.getParentId());
        }
      }
    }
    return nodes.stream().filter(node -> included.contains(node.getId())).toList();
  }

  private List<FeatureNode> collectDescendants(FeatureNode root, Map<Long, List<FeatureNode>> childrenByParent) {
    List<FeatureNode> result = new ArrayList<>();
    ArrayDeque<FeatureNode> queue = new ArrayDeque<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      FeatureNode node = queue.removeFirst();
      result.add(node);
      queue.addAll(childrenByParent.getOrDefault(node.getId(), List.of()));
    }
    return result;
  }

  private Map<Long, List<FeatureNode>> childrenByParent(List<FeatureNode> nodes) {
    Map<Long, List<FeatureNode>> result = new HashMap<>();
    for (FeatureNode node : nodes) {
      result.computeIfAbsent(node.getParentId(), ignored -> new ArrayList<>()).add(node);
    }
    return result;
  }

  private void reorderSiblings(Long productLineId, Long parentId) {
    List<FeatureNode> siblings = nodeRepository.findSiblings(productLineId, parentId);
    applySiblingOrder(siblings);
    nodeRepository.saveAll(siblings);
  }

  private void applySiblingOrder(List<FeatureNode> siblings) {
    for (int i = 0; i < siblings.size(); i++) {
      siblings.get(i).setSortOrder(i);
    }
  }

  private void applyBlockFields(FeatureContentBlock block, SaveContentBlockRequest request) {
    block.setBlockType(request.blockType());
    block.setTitle(normalizeText(request.title()));
    block.setContent(request.content());
    block.setMetadata(request.metadata());
    block.setSourceRef(normalizeText(request.sourceRef()));
  }

  private void writeHistory(Long featureId, String operationType, String description) {
    FeatureHistory history = new FeatureHistory();
    history.setId(idGenerator.nextId());
    history.setFeatureId(featureId);
    history.setOperationType(operationType);
    history.setDescription(description);
    history.setOperatorId(mockUserId);
    historyRepository.save(history);
  }

  private void validateNodeType(String nodeType) {
    if (!NODE_TYPES.contains(nodeType)) {
      throw new BusinessException("INVALID_NODE_TYPE", "节点类型只支持 module 或 feature");
    }
  }

  private void validateBlockType(String blockType) {
    if (!BLOCK_TYPES.contains(blockType)) {
      throw new BusinessException("INVALID_BLOCK_TYPE", "内容块类型只支持 overview/rule/field/api/screenshot");
    }
  }

  private String normalizeText(String text) {
    return text == null ? null : text.trim();
  }

  private FeatureNodeDto toNodeDto(FeatureNode node, List<FeatureNodeDto> children, List<FeatureContentBlockDto> blocks) {
    return new FeatureNodeDto(
        node.getId(),
        node.getProductLineId(),
        node.getParentId(),
        node.getName(),
        node.getDescription(),
        node.getNodeType(),
        node.getStatus(),
        node.getSortOrder(),
        children,
        blocks);
  }

  private FeatureContentBlockDto toBlockDto(FeatureContentBlock block) {
    return new FeatureContentBlockDto(
        block.getId(),
        block.getFeatureId(),
        block.getBlockType(),
        block.getTitle(),
        block.getContent(),
        block.getMetadata(),
        block.getSourceRef(),
        block.getSortOrder(),
        block.getCreatedAt(),
        block.getUpdatedAt());
  }

  private Comparator<FeatureNode> nodeComparator() {
    return Comparator.comparing(FeatureNode::getSortOrder, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(FeatureNode::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
  }

  private void sortMutable(List<MutableNode> nodes) {
    nodes.sort(Comparator.comparing((MutableNode mutable) -> mutable.node.getSortOrder(), Comparator.nullsLast(Integer::compareTo))
        .thenComparing(mutable -> mutable.node.getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder())));
    nodes.forEach(node -> sortMutable(node.children));
  }

  private final class MutableNode {
    private final FeatureNode node;
    private final List<MutableNode> children = new ArrayList<>();
    private final List<FeatureContentBlockDto> contentBlocks;

    private MutableNode(FeatureNode node, List<FeatureContentBlockDto> contentBlocks) {
      this.node = node;
      this.contentBlocks = contentBlocks;
    }

    private FeatureNodeDto toDto() {
      return FeatureLibraryService.this.toNodeDto(node, children.stream().map(MutableNode::toDto).toList(), contentBlocks);
    }
  }
}
