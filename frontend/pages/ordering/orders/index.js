const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiOrdersScene } = require('../../../utils/api-scenes');
const { loadFamilyBundle } = require('../../../utils/family-api');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');
const { summarizeFamilyOrders, filterFamilyOrders } = require('../../../utils/family-order-presentation');
const STATUS_OPTIONS=[{key:'all',label:'全部订单'},{key:'pending',label:'待处理'},{key:'preparing',label:'制作中'},{key:'completed',label:'已完成'}];

const { withBranding } = require('../../../utils/branding'); Page(withBranding({
  identityLoad:createIdentityLoadGuard(),
  data:{orders:[],visibleOrders:[],context:null,phase:'loading',errorMessage:'',
    pendingCount:0,preparingCount:0,completedCount:0,monthlyCount:0,statusIndex:0,statusOptions:STATUS_OPTIONS},
  onShow(){this.load();},
  retryLoad(){return this.load();},
  async load(){
    const session=requireSession({familyOnly:true});
    if(!session)return;
    const loadToken=this.identityLoad.begin(session);
    this.setData({phase:'loading',errorMessage:'',orders:[],visibleOrders:[],context:null});
    const runtime=createApiRuntime();
    try{
      const [bundle, orders, menuItems]=await Promise.all([
        loadFamilyBundle(runtime),runtime.orders.listOrders(),
        runtime.family.getMenuItems().catch(()=>[])
      ]);
      const sorted=[...orders].sort((a,b)=>String(b.expectedMealTime || b.serviceDate || '').localeCompare(String(a.expectedMealTime || a.serviceDate || '')) || Number(b.orderId)-Number(a.orderId));
      const scene=buildApiOrdersScene({homeData:bundle.homeData,orders:sorted,menuItems,imageBaseUrl:runtime.baseUrl});
      if(!this.identityLoad.isCurrent(loadToken))return;
      const summary=summarizeFamilyOrders(orders,bundle.homeData.serviceDate);
      this.setData({...scene,...summary,phase:'ready',errorMessage:'',
        visibleOrders:filterFamilyOrders(scene.orders,STATUS_OPTIONS[this.data.statusIndex].key)});
    }catch(error){
      if(!this.identityLoad.isCurrent(loadToken))return;
      this.setData({phase:'error',errorMessage:error.message || '订单列表加载失败'});
      showApiError(error,'订单列表加载失败');
    }
  },
  selectStatus(event){
    const index=STATUS_OPTIONS.findIndex(option=>option.key===event.currentTarget.dataset.filter);
    this.applyStatusFilter(index);
  },
  changeStatusFilter(event){this.applyStatusFilter(Number(event.detail.value));},
  applyStatusFilter(index){
    if(!STATUS_OPTIONS[index])return;
    this.setData({statusIndex:index,visibleOrders:filterFamilyOrders(this.data.orders,STATUS_OPTIONS[index].key)});
  },
  openDetail(event){wx.navigateTo({url:'/pages/ordering/order-detail/index?id='+event.currentTarget.dataset.id});},
  openOrderRow(event){wx.navigateTo({url:'/pages/ordering/order-detail/index?id='+event.detail.order.id});}
}));
