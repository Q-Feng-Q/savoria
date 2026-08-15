Component({properties:{item:{type:Object,value:{}},checked:{type:Boolean,value:false}},methods:{toggle(){this.triggerEvent('toggle',{item:this.data.item,checked:!this.data.checked})}}});
