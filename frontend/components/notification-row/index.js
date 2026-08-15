Component({properties:{item:{type:Object,value:{}}},methods:{open(){this.triggerEvent('open',{item:this.data.item})},read(){this.triggerEvent('read',{item:this.data.item})}}});
