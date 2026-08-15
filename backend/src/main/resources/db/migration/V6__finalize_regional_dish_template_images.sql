-- 修正地域特色菜模板的本地图片授权元数据。
-- V5 已发布，本迁移仅追加更新，不修改历史迁移。

UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/504366900',image_author='avlxyz',image_license='BY-SA' WHERE id=199 AND template_code='DISH_199';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/9751325@N02/8661289994',image_author='kudumomo',image_license='BY' WHERE id=203 AND template_code='DISH_203';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/87117631@N00/17919269439',image_author='Gary Soup',image_license='BY' WHERE id=209 AND template_code='DISH_209';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2403231552',image_author='avlxyz',image_license='BY-SA' WHERE id=214 AND template_code='DISH_214';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2300203732',image_author='avlxyz',image_license='BY-SA' WHERE id=215 AND template_code='DISH_215';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/30760976@N04/36618285890',image_author='anokarina',image_license='BY-SA' WHERE id=216 AND template_code='DISH_216';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/47038415@N00/223729056',image_author='Augapfel',image_license='BY' WHERE id=218 AND template_code='DISH_218';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2704734055',image_author='avlxyz',image_license='BY-SA' WHERE id=222 AND template_code='DISH_222';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/4956296651',image_author='avlxyz',image_license='BY-SA' WHERE id=224 AND template_code='DISH_224';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2705558736',image_author='avlxyz',image_license='BY-SA' WHERE id=227 AND template_code='DISH_227';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2144889517',image_author='avlxyz',image_license='BY-SA' WHERE id=232 AND template_code='DISH_232';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/2921569108',image_author='avlxyz',image_license='BY-SA' WHERE id=233 AND template_code='DISH_233';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/33993074@N00/3144578977',image_author='joyosity',image_license='BY' WHERE id=236 AND template_code='DISH_236';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/10559879@N00/3011561831',image_author='avlxyz',image_license='BY-SA' WHERE id=237 AND template_code='DISH_237';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/35034346243@N01/3548645229',image_author='stu_spivack',image_license='BY-SA' WHERE id=238 AND template_code='DISH_238';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/35034346243@N01/9336352485',image_author='stu_spivack',image_license='BY-SA' WHERE id=240 AND template_code='DISH_240';
UPDATE dish_templates SET image_source_url='https://www.flickr.com/photos/71834709@N00/8546361421',image_author='Colin ZHU',image_license='BY-SA 2.0' WHERE id=239 AND template_code='DISH_239';
