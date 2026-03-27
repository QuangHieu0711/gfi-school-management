import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Tree } from './tree';

describe('Tree', () => {
  let component: Tree;
  let fixture: ComponentFixture<Tree>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tree]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Tree);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
