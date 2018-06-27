import { Component, ElementRef, Input, OnInit, Renderer2, ViewChild } from '@angular/core';

import '../../../../node_modules/svg-pan-zoom';
import { Subject } from 'rxjs';

declare var Viz: any;

@Component({
  selector: 'zoom-viz',
  templateUrl: './zoom-viz.component.html',
  styleUrls: ['./zoom-viz.component.css']
})
export class ZoomVizComponent implements OnInit {
  @Input() private dotSubject: Subject<string>;
  @ViewChild('vizCanvas') private vizCanvasRef: ElementRef;
  private vizCanvas: HTMLElement;
  private zoom?: SvgPanZoom.Instance;
  private isLocked = false;
  private removeMoveListener: () => void = (() => undefined);
  private isInitialized = false;

  constructor(private renderer: Renderer2) {}

  get locked(): boolean {
    return this.isLocked;
  }

  ngOnInit(): void {
    this.vizCanvas = this.vizCanvasRef.nativeElement;
    const container = document.createElement('div');
    this.vizCanvas.appendChild(container);
    this.dotSubject.subscribe(dot => {
      // remove old mousemove listener that relies on old dimensions
      this.removeMoveListener();
      // replace old graph with new one
      container.innerHTML = Viz(dot);
      const svgElement = this.vizCanvas.getElementsByTagName('svg')[0];
      let zoomFactor = 1.;
      const someNode = svgElement.querySelector('g.node');
      if (someNode !== null) {
        zoomFactor = 50 / (someNode as HTMLElement).getBoundingClientRect().height;
      }
      // tslint:disable-next-line:curly
      if (zoomFactor > 1.) {
        // lazy init on first mouseenter event
        const mouseEnterListener = this.renderer.listen(this.vizCanvas, 'mouseenter', () => {
          // listener removes itself upon first invocation, equivalent to jQuery's once()
          mouseEnterListener();
          this.zoom = svgPanZoom(svgElement, {
            panEnabled: false,
            zoomEnabled: false,
            dblClickZoomEnabled: false,
            mouseWheelZoomEnabled: false,
            maxZoom: Math.max(10, zoomFactor)
          });
          let canvasRect = this.vizCanvas.getBoundingClientRect();
          const svgRect = svgElement.getBoundingClientRect();
          const canvasPad = (canvasRect.width - svgRect.width) / 2;
          const zoomRect = (svgElement.firstChild as HTMLElement).getBoundingClientRect();
          const panXFactor = -(zoomFactor - 1);
          const panYFactor = -(zoomRect.height / svgRect.height * zoomFactor - 1);
          this.removeMoveListener = this.renderer.listen(this.vizCanvas, 'mousemove', (e: MouseEvent) => {
            if (!this.isLocked) {
              canvasRect = this.vizCanvas.getBoundingClientRect();
              const x = e.x - canvasRect.left - canvasPad;
              const y = e.y - canvasRect.top - canvasPad;
              // console.log((e.x - cRect.left) + " => " + Math.min(Math.max(0, x), svgRect.width));
              // console.log((e.y - cRect.top) + " => " + Math.min(Math.max(0, y), svgRect.height));
              this.zoom.zoom(zoomFactor);
              this.zoom.pan({
                x: panXFactor * Math.min(Math.max(0, x), svgRect.width),
                y: panYFactor * Math.min(Math.max(0, y), svgRect.height)
              });
            }
          });
          // this must not run more than once, as events are registered on the unchanged vizCanvas element
          if (!this.isInitialized) {
            this.isInitialized = true;
            this.renderer.listen(this.vizCanvas, 'mouseleave', () => {
              if (!this.isLocked && this.zoom) {
                this.zoom.reset();
              }
            });
            this.renderer.listen(this.vizCanvas, 'click', () => {
              this.isLocked = !this.isLocked;
            });
          }
        });
      }
    });
  }
}
