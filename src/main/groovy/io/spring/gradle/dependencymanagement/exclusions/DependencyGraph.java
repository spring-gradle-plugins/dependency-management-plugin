/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.exclusions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A model of a dependency graph
 *
 * @author Andy Wilkinson
 */
public class DependencyGraph {
	public static DependencyGraphNode create(ResolvedComponentResult root,
			final Exclusions dependencyManagementExclusions) {
		return visit(root, new GraphNodeCallback() {
			public DependencyGraphNode handle(final ResolvedComponentResult component,
					DependencyGraphNode previous) {
				DependencyGraphNode current = previous;
				String id = component.getModuleVersion().getGroup() + ":" + component
						.getModuleVersion().getName();
				while (current != null) {
					if (current.getExclusions() != null && current.getExclusions().contains(id)
							|| current.getId().equals(id)) {
						return null;
					}
					current = current.getParent();
				}

				DependencyGraphNode node = new DependencyGraphNode(id, previous,
						component,
						dependencyManagementExclusions.exclusionsForDependency(id));
				if (previous != null) {
					previous.getChildren().add(node);
				}

				return node;
			}
		});
	}

	interface GraphNodeCallback {
		DependencyGraphNode handle(ResolvedComponentResult component,
				DependencyGraphNode previous);
	}

	private static DependencyGraphNode visit(ResolvedComponentResult component,
			GraphNodeCallback callback) {
		return doVisit(new HashSet<String>(), component, null, callback);
	}

	private static DependencyGraphNode doVisit(Set<String> seen,
			ResolvedComponentResult component, DependencyGraphNode previous,
			GraphNodeCallback callback) {
		DependencyGraphNode result = callback.handle(component, previous);
		if (result != null) {
			Set<? extends DependencyResult> dependencies = component.getDependencies();
			for (DependencyResult dependencyResult : dependencies) {
				if (dependencyResult instanceof ResolvedDependencyResult) {
					ResolvedComponentResult child = ((ResolvedDependencyResult) dependencyResult)
							.getSelected();
					doVisit(seen, child, result, callback);
				}

			}
			return result;
		}
		return previous;
	}

	private static Logger log = LoggerFactory.getLogger(DependencyGraph.class);

	public static class DependencyGraphNode {

		private static final Comparator<DependencyGraphNode> NODE_COMPARATOR = new Comparator<DependencyGraphNode>() {
			@Override
			public int compare(DependencyGraphNode o1, DependencyGraphNode o2) {
				return o1.depth - o2.depth;
			}
		};

		final DependencyGraphNode parent;

		final ResolvedComponentResult dependency;

		final String id;

		final List<DependencyGraphNode> children = new ArrayList<DependencyGraphNode>();

		final int depth;

		final Set<String> exclusions;

		public DependencyGraphNode(String id, DependencyGraphNode parent,
				ResolvedComponentResult dependency, Set<String> exclusions) {
			this.parent = parent;
			if (this.parent != null) {
				this.depth = parent.depth + 1;
			}
			else {
				this.depth = 0;
			}
			this.dependency = dependency;
			this.id = id;
			this.exclusions = exclusions;
		}

		public void applyExclusions(Map<String, Exclusions> dependencyExclusions) {
			doApplyExclusions(dependencyExclusions, new HashSet<String>());
		}

		private void doApplyExclusions(final Map<String, Exclusions> dependencyExclusions,
				Set<String> exclusionsFromAncestors) {
			exclusionsFromAncestors = new HashSet<String>(exclusionsFromAncestors);
			if (parent != null) {
				Exclusions exclusions = dependencyExclusions.get(parent.id);
				Set<String> exclusionsForDependency = exclusions != null ?
						exclusions.exclusionsForDependency(id) :
						null;
				if (exclusionsForDependency != null && !exclusionsForDependency.isEmpty()
						|| !exclusionsFromAncestors
						.isEmpty()) {
					Iterator<DependencyGraphNode> each = children.iterator();
					while (each.hasNext()) {
						DependencyGraphNode child = each.next();
						if (exclusionsForDependency != null && exclusionsForDependency
								.contains(child.id)
								|| exclusionsFromAncestors.contains(child.id)) {
							log.debug("Excluding " + child.id + " from " + id);
							each.remove();
						}

					}

				}

				if (exclusionsForDependency != null && !exclusionsForDependency
						.isEmpty()) {
					exclusionsFromAncestors.addAll(exclusionsForDependency);
				}
			}

			for (DependencyGraphNode node : children) {
				node.doApplyExclusions(dependencyExclusions, exclusionsFromAncestors);
			}
		}

		public void prune() {
			Map<String, List<DependencyGraphNode>> nodesById = new LinkedHashMap<String, List<DependencyGraphNode>>();
			collectById(nodesById);
			for (List<DependencyGraphNode> nodes : nodesById.values()) {
				Collections.sort(nodes, NODE_COMPARATOR);
				nodes.remove(0);
				for (DependencyGraphNode node : nodes) {
					node.parent.children.remove(node);
				}
			}
		}

		private void collectById(final Map<String, List<DependencyGraphNode>> nodesById) {
			List<DependencyGraphNode> nodes = nodesById.get(id);
			if (nodes == null) {
				nodes = new ArrayList<DependencyGraphNode>();
				nodesById.put(id, nodes);
			}
			nodes.add(this);
			for (DependencyGraphNode it : children) {
				it.collectById(nodesById);
			}
		}

		public String toString() {
			return id + " " + String.valueOf(depth);
		}

		public DependencyGraphNode getParent() {
			return parent;
		}

		public ResolvedComponentResult getDependency() {
			return dependency;
		}

		public String getId() {
			return id;
		}

		public List<DependencyGraphNode> getChildren() {
			return children;
		}

		public int getDepth() {
			return depth;
		}

		public Set<String> getExclusions() {
			return exclusions;
		}
	}
}
