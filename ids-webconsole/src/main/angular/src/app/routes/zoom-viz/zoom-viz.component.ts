import { Component, ElementRef, Input, OnInit, Renderer2, ViewChild } from '@angular/core';
import { from, Subject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import 'svg-pan-zoom';

/* eslint-disable @typescript-eslint/naming-convention */
declare const Viz: any;

@Component({
  selector: 'zoom-viz',
  templateUrl: './zoom-viz.component.html',
  styleUrls: ['./zoom-viz.component.css']
})
export class ZoomVizComponent implements OnInit {
  @Input() private readonly dotSubject: Subject<string>;
  @ViewChild('vizCanvas', { static: true }) private readonly vizCanvasRef: ElementRef;
  private zoom?: SvgPanZoom.Instance;
  private isLocked = false;
  private isInitialized = false;

  constructor(private readonly renderer: Renderer2) {}

  public ngOnInit(): void {
    const viz = new Viz();
    const vizCanvas = this.vizCanvasRef.nativeElement;
    const container = document.createElement('div');
    vizCanvas.appendChild(container);
    this.dotSubject.pipe(switchMap(dot => from(viz.renderSVGElement(dot) as Promise<SVGElement>)))
      .subscribe(svgElement => {
        // remove old mousemove listener that relies on old dimensions
        this.removeMoveListener();
        // replace old graph with new one
        container.innerHTML = '';
        container.appendChild(svgElement);
        let zoomFactor = 1;
        const someNode = svgElement.querySelector('g.node');
        if (someNode !== null) {
          zoomFactor = 50 / (someNode as HTMLElement).getBoundingClientRect().height;
        }
        // eslint-disable-next-line curly
        if (zoomFactor > 1) {
          // lazy init on first mouseenter event
          const mouseEnterListener = this.renderer.listen(vizCanvas, 'mouseenter', () => {
            // listener removes itself upon first invocation, equivalent to jQuery's once()
            mouseEnterListener();
            this.zoom = svgPanZoom(svgElement, {
              panEnabled: false,
              zoomEnabled: false,
              dblClickZoomEnabled: false,
              mouseWheelZoomEnabled: false,
              maxZoom: Math.max(10, zoomFactor)
            });
            let canvasRect = vizCanvas.getBoundingClientRect();
            const svgRect = svgElement.getBoundingClientRect();
            const canvasPad = (canvasRect.width - svgRect.width) / 2;
            const zoomRect = (svgElement.firstChild as HTMLElement).getBoundingClientRect();
            const panXFactor = -(zoomFactor - 1);
            const panYFactor = -(zoomRect.height / svgRect.height * zoomFactor - 1);
            this.removeMoveListener = this.renderer.listen(vizCanvas, 'mousemove', (e: MouseEvent) => {
              if (!this.isLocked) {
                canvasRect = vizCanvas.getBoundingClientRect();
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
              this.renderer.listen(vizCanvas, 'mouseleave', () => {
                if (!this.isLocked && this.zoom) {
                  this.zoom.reset();
                }
              });
              this.renderer.listen(vizCanvas, 'click', () => {
                this.isLocked = !this.isLocked;
              });
            }
          });
        }
      });
  }

  private removeMoveListener: () => void = (() => undefined);

  get locked(): boolean {
    return this.isLocked;
  }

}
