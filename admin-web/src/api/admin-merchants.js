import { normalizeArray,request } from './http';
export const listAdminMerchants=async(keyword='')=>normalizeArray(await request(`/api/admin/merchants${keyword?`?keyword=${encodeURIComponent(keyword)}`:''}`,{method:'GET'}));
export const createAdminMerchant=payload=>request('/api/admin/merchants',{method:'POST',body:JSON.stringify(payload),headers:{'Content-Type':'application/json'}});
export const updateAdminMerchant=(id,payload)=>request(`/api/admin/merchants/${id}`,{method:'PUT',body:JSON.stringify(payload),headers:{'Content-Type':'application/json'}});
export const deleteAdminMerchant=id=>request(`/api/admin/merchants/${id}`,{method:'DELETE'});
export const rotateMerchantInvitationCode=id=>request(`/api/admin/merchants/${id}/invitation-code`,{method:'POST'});
export const disableMerchantInvitationCode=id=>request(`/api/admin/merchants/${id}/invitation-code`,{method:'DELETE'});
