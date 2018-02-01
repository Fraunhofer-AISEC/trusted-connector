import { Component, Input, OnInit, ElementRef, ViewChild, Renderer2 } from '@angular/core';

import '../../../../node_modules/svg-pan-zoom';
import { Subject } from 'rxjs/Subject';

declare var Viz: any;

@Component({
  selector: 'zoom-viz',
  templateUrl: './zoom-viz.component.html',
  styleUrls: ['./zoom-viz.component.css']
})
export class ZoomVizComponent implements OnInit {
  @Input() private dotSubject: Subject<string>;
  @Input() private dot: string;
  @ViewChild('vizCanvas') private vizCanvasRef: ElementRef;
  @ViewChild('iLock') private lockIcon: ElementRef;
  private vizCanvas: HTMLElement;
  private svgElement: SVGSVGElement;
  private isLocked = false;

  constructor(private renderer: Renderer2) {}

  get locked() {
    return this.isLocked;
  }

  ngOnInit(): void {
    this.vizCanvas = this.vizCanvasRef.nativeElement;
    this.dotSubject.subscribe((dot) => {
      let container = document.createElement('div');
      container.innerHTML = Viz(dot);
      while (this.vizCanvas.firstChild) {
        this.vizCanvas.removeChild(this.vizCanvas.firstChild);
      }
      this.vizCanvas.appendChild(container);
      this.svgElement = this.vizCanvas.getElementsByTagName('svg')[0];
      let zoomFactor = 1.;
      let someNode = this.svgElement.querySelector('g.node');
      if (someNode !== null) {
        zoomFactor = 50 / (someNode as HTMLElement).getBoundingClientRect().height;
      }
      if (zoomFactor > 1.) {
        let mouseEnterListener = this.renderer.listen(this.vizCanvas, 'mouseenter', (e: MouseEvent) => {
          // listener removes itself on first call
          mouseEnterListener();
          let zoom = svgPanZoom(this.svgElement, {
            panEnabled: false,
            zoomEnabled: false,
            dblClickZoomEnabled: false,
            mouseWheelZoomEnabled: false,
            maxZoom: Math.max(10, zoomFactor)
          });
          let canvasRect = this.vizCanvas.getBoundingClientRect();
          let svgRect = this.svgElement.getBoundingClientRect();
          let canvasPad = (canvasRect.width - svgRect.width) / 2;
          let zoomRect = (this.svgElement.firstChild as HTMLElement).getBoundingClientRect();
          let panXFactor = -(zoomFactor - 1);
          let panYFactor = -(zoomRect.height / svgRect.height * zoomFactor - 1);
          this.renderer.listen(this.vizCanvas, 'mousemove', (e: MouseEvent) => {
            if (!this.isLocked) {
              canvasRect = this.vizCanvas.getBoundingClientRect();
              let x = e.x - canvasRect.left - canvasPad, y = e.y - canvasRect.top - canvasPad;
              // console.log((e.x - cRect.left) + " => " + Math.min(Math.max(0, x), svgRect.width));
              // console.log((e.y - cRect.top) + " => " + Math.min(Math.max(0, y), svgRect.height));
              zoom.zoom(zoomFactor);
              zoom.pan({
                x: panXFactor * Math.min(Math.max(0, x), svgRect.width),
                y: panYFactor * Math.min(Math.max(0, y), svgRect.height)
              });
            }
          });
          this.renderer.listen(this.vizCanvas, 'mouseleave', (e: MouseEvent) => {
            if (!this.isLocked) {
              zoom.reset();
            }
          });
          this.renderer.listen(this.vizCanvas, 'click', (e: MouseEvent) => {
            this.isLocked = !this.isLocked;
          });
        });
      }
    });
  }
}
