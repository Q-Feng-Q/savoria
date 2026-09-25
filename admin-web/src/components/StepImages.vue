<template>
  <div v-if="editable || modelValue.length" class="step-images">
    <small v-if="editable">步骤图片 {{ modelValue.length }}/5 · 选填，单张最多 4MB</small>
    <div class="step-images__grid">
      <div v-for="(url,index) in modelValue" :key="`${index}-${url}`" class="step-images__item">
        <button type="button" class="step-images__preview" @click="previewIndex=index"><img :src="assetUrl(url)" :alt="`步骤图片 ${index+1}，点击放大`" /></button>
        <button v-if="editable" type="button" class="step-images__remove" :disabled="disabled || busy" @click="remove(index)">删除</button>
      </div>
      <label v-if="editable && modelValue.length < 5" class="step-images__add" :class="{'is-disabled':disabled || busy}">
        <input type="file" accept="image/jpeg,image/png,image/webp" multiple :disabled="disabled || busy" @change="choose" />
        {{ busy ? '上传中…' : '＋ 添加图片' }}
      </label>
    </div>
    <Teleport to="body">
      <div v-if="previewIndex !== null && modelValue[previewIndex]" class="step-images__lightbox" role="dialog" aria-modal="true" aria-label="步骤图片预览" @click.self="previewIndex=null" @keydown.esc="previewIndex=null" tabindex="-1">
        <button type="button" class="step-images__close" @click="previewIndex=null">关闭预览 ×</button>
        <img :src="assetUrl(modelValue[previewIndex])" alt="步骤图片放大预览" />
        <div class="step-images__paging"><button type="button" :disabled="previewIndex===0" @click="previewIndex--">上一张</button><span>{{previewIndex+1}} / {{modelValue.length}}</span><button type="button" :disabled="previewIndex===modelValue.length-1" @click="previewIndex++">下一张</button></div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue';
import { uploadDishImage } from '../api/files';
import { getApiBaseUrl, getStoredSession } from '../api/http';
import { uploadStepImages, stepImageUrl } from '../utils/step-images';
import { notify } from '../utils/feedback';
const props=defineProps({modelValue:{type:Array,default:()=>[]},editable:Boolean,disabled:Boolean});
const emit=defineEmits(['update:modelValue','busy']);
const busy=ref(false),previewIndex=ref(null);
let alive=true;
onBeforeUnmount(()=>{alive=false;});
const assetUrl=url=>stepImageUrl(url,getApiBaseUrl());
function remove(index){if(!props.disabled&&!busy.value)emit('update:modelValue',props.modelValue.filter((_,i)=>i!==index));}
async function choose(event){
  const files=Array.from(event.target.files || []);event.target.value='';
  if(!files.length||props.disabled||busy.value)return;
  const identity=JSON.stringify(getStoredSession()),base=getApiBaseUrl();
  const isCurrent=()=>alive&&identity===JSON.stringify(getStoredSession())&&base===getApiBaseUrl();
  busy.value=true;emit('busy',true);
  try{
    const result=await uploadStepImages({existing:props.modelValue,files,upload:uploadDishImage,isCurrent});
    if(!result.stale&&isCurrent()){
      emit('update:modelValue',result.images);
      if(result.failed)notify(`${result.failed} 张上传失败，请重试（单张最多 4MB）`,'error');
    }
  }catch(error){if(isCurrent())notify(error.message || '上传失败','error');}
  finally{if(alive){busy.value=false;emit('busy',false);}}
}
</script>

<style scoped>
.step-images{grid-column:1/-1;min-width:0;margin-top:10px}.step-images>small{display:block;color:#806443;margin-bottom:8px}.step-images__grid{display:flex;flex-wrap:wrap;gap:10px}.step-images__item{width:92px;max-width:100%}.step-images__preview{display:block;width:100%;padding:0;border:0;background:#f1dfb9;border-radius:12px;overflow:hidden;cursor:zoom-in}.step-images__preview img{display:block;width:100%;height:92px;object-fit:cover}.step-images__remove{width:100%;border:0;background:transparent;color:#a44229;padding:7px;cursor:pointer}.step-images__add{display:flex;align-items:center;justify-content:center;position:relative;width:92px;height:92px;border:1px dashed #b89968;border-radius:12px;background:#f8eacb;color:#654627;font-size:12px;cursor:pointer}.step-images__add input{position:absolute;width:100%;height:100%;opacity:0;cursor:pointer}.is-disabled{opacity:.5}.step-images__lightbox{position:fixed;inset:0;z-index:10000;background:rgba(45,30,18,.92);display:flex;flex-direction:column;align-items:center;justify-content:center;padding:24px;gap:16px}.step-images__lightbox>img{max-width:92vw;max-height:75vh;object-fit:contain}.step-images__close,.step-images__paging button{background:#fff0d1;color:#51331f;border:0;padding:12px 18px;border-radius:10px;cursor:pointer}.step-images__paging{display:flex;align-items:center;gap:16px;color:#fff0d1}.step-images__paging button:disabled{opacity:.4}
</style>
