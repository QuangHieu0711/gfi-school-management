import { NgTemplateOutlet } from '@angular/common';
import { AfterContentChecked, ChangeDetectorRef, Component, EventEmitter, input, Input, OnInit, Output, TemplateRef, ViewChild } from '@angular/core';
import { Tree, TreeModule } from 'primeng/tree';

@Component({
  selector: 'app-core-tree',
  imports: [
    TreeModule,
    NgTemplateOutlet
  ],
  templateUrl: './tree.html',
  styleUrl: './tree.scss',
})
export class CoreTree implements OnInit, AfterContentChecked {
  _config: any;
  dataTree = input<any[]>([]);
  hasTemplate = input<boolean>(false);
  /** Map node key -> true để mở rộng node. Truyền object với tất cả key = true để mặc định mở hết. */
  @Input() expandedKeys: Record<string, boolean> = {};
  // @Input() dataTree: any = [];
  // @Input() hasTemplate: boolean = false;
  @Input() myContext = {};
  @Input() dragNode: boolean = false;
  @Input() dropNode: boolean = false;
  currentNode: any = null;
  @ViewChild('treeMenu') treeMenu?: Tree;
  @Output() selectNode: EventEmitter<any> = new EventEmitter();
  @Output() unSelectNode: EventEmitter<any> = new EventEmitter();
  @Output() nodeSelectedModelChange = new EventEmitter();
  @Output() nodeExpandEvt = new EventEmitter();
  @Output() nodeCollapseEvt = new EventEmitter();
  @Output() nodeDropEvt = new EventEmitter();
  @Output() treeFilterEvt = new EventEmitter();
  @Input()
  get treeConfig(): any {
    return this._config;
  }
  set treeConfig(config: any) {
    this._config = config;
    this.setConfigTree(config);
  }



  private _templateRef!: TemplateRef<any>;
  @Input()
  get templateRef(): TemplateRef<any> {
    return this._templateRef;
  }
  set templateRef(templateRef: TemplateRef<any>) {
    this._templateRef = templateRef;
  }



  nodeSelected: any;
  _nodeSelectedModel: any;
  @Input()
  get nodeSelectedModel() {
    return this._nodeSelectedModel;
  }
  set nodeSelectedModel(nodeSelected) {
    this._nodeSelectedModel = nodeSelected;
    this.nodeSelectedModelChange.emit(this._nodeSelectedModel);
  }


  constructor(
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {

  }
  ngAfterContentChecked() {
    this.cdr.detectChanges();
  }
  setConfigTree(config: any) {
    this.cdr.detectChanges();

    Object.keys(config).forEach((key: any) => {
      if (this.treeMenu) {
        (this.treeMenu as any)[key] = config[key];
      }
    })
  }
  nodeSelect(evt: any) {
    if (this.treeConfig['selectionMode'] === 'single') {
      // Click cùng node đang chọn => bỏ chọn (toggle off)
      if (this.currentNode && evt.node === this.currentNode) {
        this.unSelectNode.emit(evt.node);
        this.currentNode = null;
        this.nodeSelectedModel = null;
        this.cdr.detectChanges();
      } else {
        this.currentNode = evt.node;
        this.nodeSelectedModel = evt.node;
        this.selectNode.emit(evt.node);
      }
    } else {
      const obj = {
        nodes: this.nodeSelectedModel,
        currentNode: evt.node
      }
      this.selectNode.emit(obj);
    }


  }
  nodeUnselect(evt: any) {
    this.currentNode = null;
    this.nodeSelectedModel = null;
    this.cdr.detectChanges();
    this.unSelectNode.emit(evt.node);
  }
  nodeExpand(evt: any) {
    this.nodeExpandEvt.emit(evt)
  }
  nodeCollapse(evt: any) {
    this.nodeCollapseEvt.emit(evt)
  }
  nodeDrop(evt: any) {
    this.nodeDropEvt.emit(evt)
  }
  treeFilter(evt: any) {
    this.treeFilterEvt.emit(evt)
  }
}
