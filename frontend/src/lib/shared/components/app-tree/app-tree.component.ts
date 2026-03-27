/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable unused-imports/no-unused-vars */
import {
  ChangeDetectionStrategy,
  Component,
  Input,
  TemplateRef,
  ViewChild,
  OnChanges,
} from '@angular/core';
import { TreeNode } from '@model/tree.models';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { MatTree } from '@angular/material/tree';
import { CommonModule } from '@angular/common';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ID_TYPE } from '@model/response.model';

@Component({
  selector: 'app-tree',
  templateUrl: './app-tree.component.html',
  imports: [CommonModule, ...MATERIAL_MODULE, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreeComponent extends ComponentBaseAbstract implements OnChanges {
  @ViewChild('tree', { static: true })
  private readonly tree!: MatTree<TreeNode>;
  @Input() dataSource: TreeNode[] = [];
  @Input() flatDataSource: TreeNode[] = [];
  @Input() idField = 'id';
  @Input() parentIdField = 'parentId';
  /**
   * label template
   */
  @Input()
  labelTemplate: TemplateRef<unknown> | null = null;

  /** This is the transformed tree structure */
  treeDataSource: TreeNode[] = [];

  childrenAccessor = (node: TreeNode) => node.children ?? [];

  hasChild = (_: number, node: TreeNode): boolean =>
    this.childrenAccessor(node)?.length > 0;

  ngOnChanges(changes: {
    dataSource?: { currentValue: TreeNode[] };
    flatDataSource?: { currentValue: TreeNode[] };
  }): void {
    if (changes.flatDataSource?.currentValue.length) {
      this.treeDataSource = this.buildTree(this.flatDataSource);
      setTimeout(() => {
        if (this.treeDataSource.length) {
          // Expand all nodes so the full tree is visible by default
          this.expandAll();
        }
      });
    } else if (changes.dataSource?.currentValue.length) {
      this.treeDataSource = this.dataSource;
      setTimeout(() => {
        if (this.treeDataSource.length) {
          // Expand all nodes so the full tree is visible by default
          this.expandAll();
        }
      });
    }
  }

  private expandAll(): void {
    const expandRecursive = (nodes: TreeNode[] | undefined) => {
      if (!nodes || !nodes.length) return;
      nodes.forEach((node) => {
        // Only expand if node has expanded property set to true
        if (node['expanded'] === true) {
          try {
            this.tree.expand(node);
          } catch (e) {
          }
        }
        if (node.children && node.children.length) {
          expandRecursive(node.children);
        }
      });
    };

    expandRecursive(this.treeDataSource);
  }

  buildTree(dataSource: TreeNode[] = []): TreeNode[] {
    const arrayToTreeRecursive = (
      flatData: TreeNode[],
      parentId?: ID_TYPE
    ): TreeNode[] =>
      flatData
        .filter((item) => item[this.parentIdField] === parentId)
        .map((item) => ({
          ...item,
          children: arrayToTreeRecursive(
            flatData,
            item[this.idField] as ID_TYPE
          ),
        }));

    const root = arrayToTreeRecursive(dataSource);
    return root.length > 0 ? root : dataSource;
  }

  /**
   * TrackBy function for efficient rendering of tree nodes
   * @param _ - Index (not used)
   * @param node - The tree node to track
   * @returns The unique identifier for the node
   */
  trackByFn = (_: number, node: TreeNode) => node[this.idField];
}
