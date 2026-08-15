Component({properties:{groups:{type:Array,value:[]}},methods:{select(event){const item=event.currentTarget.dataset.item;if(!item.disabled)this.triggerEvent('select',{item})}}});
