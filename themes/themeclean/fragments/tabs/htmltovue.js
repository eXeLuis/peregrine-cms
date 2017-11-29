module.exports = {
    convert: function($, f) {
        //f.bindPath($)
    	f.wrap($, 'themeclean-components-block')
        f.bindAttribute($.parent(),'model','model')


        f.replace( $.find('img'), '<themeclean-components-mediavisible :model="model"></themeclean-components-mediavisible>')

        let imageDiv = $.find('div.col-md-auto').first()
        f.addIf( imageDiv, 'model.showmedia == \'true\'')

        let containerClasses = `{
            'flex-row': model.medialignment === 'left',
            'flex-row-reverse': model.medialignment === 'right'
        }`
        f.bindAttribute($.find('div.row').eq(1), 'class', containerClasses, false)

        f.bindAttribute($.find('div.row>div.col').first(), 'style', "{width:`${model.mediawidth}%`}")
    	f.addIf($.find('h2').first(), 'model.showtitle == \'true\' && model.title')
        f.mapRichField($.find('h2').first(), "model.title")
        f.addIf($.find('h3').first(), 'model.showsubtitle == \'true\' && model.subtitle')
        f.mapRichField($.find('h3').first(), "model.subtitle")

    	f.addFor($.find('li.nav-item').first(), 'model.tabs')
    	f.bindAttribute($.find('a').first(), 'href', '`#tab${_uid}${parseInt(i)+1}`')
    	f.bindAttribute($.find('a').first(), 'class', 'i == 0 ? \'nav-link active\' : \'nav-link\'')
    	f.bindAttribute($.find('a').first(), 'id', '`tab-control-${_uid}${parseInt(i)+1}`')
    	f.bindAttribute($.find('a').first(), 'aria-controls', '`tab${_uid}${parseInt(i)+1}`')
    	f.mapRichField($.find('a').first(), "item.title")
    	
    	f.addFor($.find('div.tab-pane').first(), 'model.tabs')
    	f.bindAttribute($.find('div.tab-pane').first(), 'id', '`tab${_uid}${parseInt(i)+1}`')
        f.bindAttribute($.find('div.tab-pane').first(), 'aria-labelledby', '`tablabel${_uid}${parseInt(i)+1}`')
        f.bindAttribute($.find('div.tab-pane').first(), 'class', 'i == 0 ? \'tab-pane fade show active\' : \'tab-pane fade\'')
        f.mapRichField($.find('div.text-center').first(), "item.text")
        f.bindAttribute($.find('div.text-center').first(), 'class',"model.showmedia == 'true' && ((model.videosrc && model.mediatype === 'video') || (model.imagesrc && model.mediatype === 'image') || (model.mediaicon && model.mediatype === 'icon')) ? 'py-5' : 'text-center py-5'")
    	f.addIf($.find('.perIsEditAndEmpty').first(), 'isEditAndEmpty')
        
    }
}