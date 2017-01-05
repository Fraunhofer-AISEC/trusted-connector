import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';


@Component({
  selector: 'ids',
  templateUrl: 'app/ids/ids.component.html'
})

export class IdsComponent  implements OnInit{

  title = 'IDS';

  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title) {
     this.titleService.setTitle('IDS');
  }

  ngOnInit(): void {
    this.changeTitle.emit('IDS');
  }

  public editedSettings = false;

  saveSettings(): void {
     //show box msg
     this.editedSettings = true;
     //wait 3 Seconds and hide
     setTimeout(function() {
         this.editedSettings = false;
         console.log(this.editedSettings);
     }.bind(this), 3000);
  }

  public editedPipes = false;

   save(): void {
      //show box msg
      this.editedPipes = true;
      //wait 3 Seconds and hide
      setTimeout(function() {
          this.editedPipes = false;
          console.log(this.editedPipes);
      }.bind(this), 3000);
   }

   public installed = false;

    install(): void {
       //show box msg
       this.installed = true;
       //wait 3 Seconds and hide
       setTimeout(function() {
           this.installed = false;
           console.log(this.installed);
       }.bind(this), 3000);
    }
}
