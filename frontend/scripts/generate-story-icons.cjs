// Code-native icons matching the approved parchment UI. Re-run to regenerate PNG assets.
const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const output=path.resolve(__dirname,'../assets/ui/story');
const paths={
 home:'<path d="M3 11 12 3l9 8M6 10v11h5v-7h3v7h4V10"/>',
 menu:'<path d="M3 18h18M5 16a7 7 0 0 1 14 0M12 7V5M10 5h4M6 21h12"/>',
 basket:'<path d="m3 9 2 12h14l2-12H3ZM7 9l4-6M17 9l-4-6M9 13v5M15 13v5"/>',
 orders:'<rect x="5" y="3" width="14" height="19" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/>',
 profile:'<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2H4Z"/>',
 calendar:'<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v5M17 3v5M3 11h18M7 15h1M12 15h1M17 15h1M7 18h1M12 18h1"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 3"/>',
 pot:'<path d="M5 10h14v9a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-9ZM2 12h3M19 12h3M3 8h18M9 5c-2-2 1-2 0-4M14 5c-2-2 1-2 0-4"/>',
 check:'<path d="m5 12 5 5L20 6"/>',
 truck:'<path d="M2 5h12v13H2V5ZM14 10h4l4 5v3h-8M14 10v5h8"/><circle cx="6" cy="19" r="2"/><circle cx="18" cy="19" r="2"/>',
 store:'<path d="M3 9 5 3h14l2 6M3 9c0 4 4 4 4 0 0 4 5 4 5 0 0 4 5 4 5 0 0 4 4 4 4 0M5 12v9h14v-9M9 21v-6h6v6"/>',
 pin:'<path d="M12 22S4 14 4 9a8 8 0 0 1 16 0c0 5-8 13-8 13Z"/><circle cx="12" cy="9" r="3"/>',
 leaf:'<path d="M11 22c-2-8 0-14 6-20M12 12C4 12 3 7 4 4c5 0 8 4 8 8ZM12 17c0-6 5-8 9-8-1 5-4 8-9 8Z"/>'
};
async function main(){fs.mkdirSync(output,{recursive:true});for(const [name,p] of Object.entries(paths)){for(const [tone,color]of Object.entries({ink:'#805c35',green:'#688142',orange:'#e77a37',cream:'#fff4d8'})){const svg='<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24"><g fill="none" stroke="'+color+'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+p+'</g></svg>';await sharp(Buffer.from(svg)).png().toFile(path.join(output,name+'-'+tone+'.png'));}}console.log('Generated '+Object.keys(paths).length*4+' small UI icons');}
main().catch(error=>{console.error(error);process.exitCode=1;});
