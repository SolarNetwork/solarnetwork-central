SolarReg.Templates = {};

/**
* Get a display name for a service based on its identifier.
* 
* @param {Array} services the array of localized service infos to search
* @param {String} identifier the service ID to look for
* @returns {String} the localized name, or an empty string if not found
*/
SolarReg.Templates.serviceDisplayName =	function serviceDisplayName(services, identifier) {
   var service = SolarReg.findByIdentifier(services, identifier);
   return (service ? service.localizedName : '');
};

/**
 * Populate an HTML `<select>` element with options based on localized service infos.
 * 
 * Each
 * 
 * @param {Array} services array of localized service infos
 * @param {String} selector jQuery selector for the `<select>` element to update
 * @returns {Array} the `services` array
 */
SolarReg.Templates.populateServiceSelectOptions = function populateServiceSelectOptions(services, selector) {
	services = Array.isArray(services) ? services : [];
	$(selector).empty().each(function() {
		var select = this;
		services.forEach(function(service) {
			var opt = new Option(service.localizedName || service.name, service.id);
			if ( service.localizedDescription ) {
				// set title, but strip out HTML tags
				opt.title = $('<div>'+service.localizedDescription+'</div>').text();
			}
			select.add(opt);
		});
	});
	return services;
};

/**
 * Find an existing template item element in a list of items.
 * 
 * @param {jQuery} container the container of the list of items to search
 * @param {*} itemId the item ID to look for
 * @returns {jQuery} the jQuery selector for the first matching item
 */
SolarReg.Templates.findExistingTemplateItem = function findExistingTemplateItem(container, itemId) {
	return container.children().filter(function(i, e) {
		var ctx = $(e).data('context-item');
		// note the loose == here, to handle numbers that might be handed in as strings from form elements
		return (ctx && ctx.id && ctx.id == itemId);
	}).first();
};

/**
 * Create new DOM elements from a template DOM element for each of a parameter object.
 * 
 * The `container` will be searched for an element with class `list-container`, and 
 * if found that element will be where the new DOM elements will be appended. If not 
 * found, the parent of `container` will be used.
 * 
 * If `preserve` is `false`, then if `.list-container` is found its children will be
 * removed else the siblings following `.template` will be removed.
 * 
 * @param {jQuery} container the container that holds the template item
 * @param {Array} items  the array of parameter objects to populate into cloned templates
 * @param {boolean} preserve `true` to only append items, do not clear out any existing items
 * @param {Function} [callback] a callback function, will be passed an item and the jQuery object associated with that item,
 *                              after template properties have been applied
 * @see #appendTemplateItem
 */
SolarReg.Templates.populateTemplateItems = function populateTemplateItems(container, items, preserve, callback) {
	var itemTemplate = container.find('.template')
		.not('.service-props-template .template') // ignore service prop templates that might be in container
		.not('.template .template');              // ignore nested templates 
	var itemContainer = container.find('.list-container').first();
	if ( itemContainer.length < 1 ) {
		// .list-container not found; use parent as container, and clear siblings following template if !preserve
		itemContainer = itemTemplate.parent();
		if ( !preserve ) {
			itemTemplate.nextAll().remove();
		}
	} else if ( !preserve ) {
		// found .list-container and !preserve
		itemContainer.empty();
	}
	items.forEach(item => {
		var el;
		if ( preserve && item._contextItem && item._contextItem.id ) {
			// look for existing row to update, rather than append
			el = SolarReg.Templates.findExistingTemplateItem(itemContainer, item._contextItem.id);
		}
		if ( el && el.length > 0 ) {
			// clear any existing props in case values have been deleted
			el.find('[data-tprop]').text('');

			SolarReg.Templates.replaceTemplateProperties(el, item);
			SolarReg.Templates.setContextItem(el, item._contextItem);
		} else {
			el = SolarReg.Templates.appendTemplateItem(itemContainer, itemTemplate, item);
		}
		if ( typeof callback === 'function' ) {
			callback(item, el);
		}
	});
	
	// look for empty "detail list" items to hide, non-empty items to show
	container.find('dl.details-container > dd:empty').prev('dt').addBack().addClass('hidden');
	container.find('dl.details-container > dd:not(:empty)').prev('dt').addBack().removeClass('hidden');
	
	container.toggleClass('hidden', items.length < 1);
};

/**
* Replace the textual content of DOM elements who have data attributes matching template parameter names.
* 
* This method replaces parameters in a DOM structure, based on parameter names encoded as HTML
* `data-tprop` attributes. For any element with a matching `data-tprop` value, its text content will 
* be replaced by the parameter value. If the parameter value is an array, then it is treated as HTML
* instead of text. 
* 
* Imagine a DOM structure like:
* 
* ```html
* <tr><td data-tprop="name"></td></tr>
* ```
* 
* Then calling this method like `replaceTemplateProperties($(trEl), {name:"Hello, world."})` will change the DOM
* to this:
* 
* ```html
* <tr><td>Hello, world.</td></tr>
* ```
* 
* **Note** that parameter names starting with `_` are skipped.
*
* The element with the `data-tprop` attribute can also define other `data-X-text` attributes, where `X`
* is the replacement value. If available, the data attribute value will be used in the DOM, rather than
* the property value directly. For example if you have a DOM structure like this:
*
* ```html
* <tr><td data-tprop="active" data-true-text="On" data-false-text="Off"></td></tr>
* ```
* 
* Then calling this method like `replaceTemplateProperties($(trEl), {active:true})` will change the DOM
* to this:
* 
* ```html
* <tr><td>On</td></tr>
* ```
*
* The element with the `data-tprop` attribute can also define other `data-X-label` attributes, where `X`
* is the replacement value. If available, and the element also has a `label` class, then the attribute
* value will be added as a label class, and `label-default` removed.
*
* In addition, if `obj` has an object property named `serviceProperties`, special handling is performed
* to generate a dynamic list of key/value property pairs using another HTML template. The desintation
* for the dynamic HTML is determined by the element with a `.service-props-container` class. The HTML
* template for each dynamic property are the elements matching a `.service-props-template .template` 
* selector. Within the tempalte, the `serviceProperties.name` template property will be replaced
* by the service property name and `serviceProperty.value` its associated value.
*
* For example, the following HTML contains a `<dl>` container for service properties and associated
* template HTML that generates `<dt>` and `<dd>` elements:
*
* ```html
* <dl class="service-props-container">
* </dl>
* <dl class="service-props-template hidden">
*   <dt class="template" data-tprop="serviceProperties.name"></dt>
* 	<dd class="template" data-tprop="serviceProperties.value"></dd>
* </dl>
* ```
* 
* @param {jQuery} el the element to perform replacements on
* @param {object} obj the object whose properties act as template parameters
* @param {string} [prefix] an optional prefix to prepend to each template parameter
*/
SolarReg.Templates.replaceTemplateProperties = function replaceTemplateProperties(el, obj, prefix) {
	var prop, sel, val, vel, vkey, vlabel,
		sPropKey, sPropVal, sPropItem,
		sPropContainer = el.find('.service-props-container').first(),
		sPropTemplate = el.find('.service-props-template .template');
	for ( prop in obj ) {
		if ( prop.startsWith('_') || !obj.hasOwnProperty(prop) ) {
			continue;
		}
		val = obj[prop];
		if ( prop === 'serviceProperties' && sPropContainer.length > 0 ) {
            sPropContainer.empty();
			for ( sPropKey in val ) {
				if ( !val.hasOwnProperty(sPropKey) ) {
					continue;
				}
				sPropVal = val[sPropKey];
				sPropItem = sPropTemplate.clone(true).removeClass('template');
				sel = "[data-tprop='" +(prefix || '') +"serviceProperties.name']";
				sPropItem.find(sel).addBack(sel).text(sPropKey);
				sel = "[data-tprop='" +(prefix || '') +"serviceProperties.value']";
				sPropItem.find(sel).addBack(sel).text(sPropVal);
				sPropContainer.append(sPropItem);
			}
		} else {
			sel = "[data-tprop='" +(prefix || '') +prop +"']";
			if ( Array.isArray(val) ) {
				el.find(sel).addBack(sel).html(val);
			} else {
				vel = el.find(sel).addBack(sel);
				if ( vel.hasClass('label') ) {
					// for boolean values, toggle 'success' and 'default' label classes if 'label' class exists
					if ( typeof val == 'boolean' ) {
						vel.toggleClass('label-success', val);
						vel.toggleClass('label-default', !val);
					} else {
						vkey = String(val).toLowerCase()+'-label';
						vlabel = vel.data(vkey);
						vel.toggleClass('label-default', vlabel === undefined);
						if ( vlabel ) {
							vel.addClass('label-' + vlabel);
						}
					}
				}
				// look for a data property named the lower-case value + '-text' for i18n message replacement
				vkey = String(val).toLowerCase()+'-text';
				vel.text(vel.data(vkey) ? vel.data(vkey) : val);
			}
		}
	}
	return el;
};

/**
* Clone a template element, replace template parameters in the clone, and append
* the clone to a container element.
* 
* The special `_contextItem` parameter value, if provided, will be stored on 
* the cloned element as a data attribute named `context-item`.
* 
* @param {jQuery} container the container element to append the new item into
* @param {jQuery} template the template to clone
* @param {object} item the parameter object
* @returns {jQuery} the newly inserted element
*/
SolarReg.Templates.appendTemplateItem = function appendTemplateItem(container, template, item) {
   if ( !(container && template && item) ) {
	   return;
   }
   var newItem = template.clone(true).removeClass('template');
   if ( item._contextItem ) {
	   SolarReg.Templates.setContextItem(newItem, item._contextItem);
   }
   SolarReg.Templates.replaceTemplateProperties(newItem, item).appendTo(container);
   newItem.find('a.edit-link').on('click', function(event) {
	   // don't handle this directly; assume will propagate and get handled at container level
	   event.preventDefault();
   });
   return newItem;
};

SolarReg.Templates.serviceInfoMessage = function serviceInfoMessage(service, key) {
	if ( !(service && service.localizedInfoMessages && key) ) {
		return '';
	}
	return (service.localizedInfoMessages[key] || '');
};

/**
 * Search for the nearest configuration container for an element.
 * 
 * The configuration container is the closest element (anscestor or self) that 
 * contains the class `configs`. If not found, `el` itself will be returned.
 * 
 * @param {jQuery} el the element to find the configuration container for
 */
SolarReg.Templates.findConfigurationContainer = function findConfigurationContainer(el) {
	if ( el.hasClass('configs') ) {
		return el;
	}
	var result = el.closest('.configs');
	return (result.length > 0 ? result : el);
};

/**
 * Find the first available context item for a DOM element.
 * 
 * @param {element} el the element to find the context item for
 * @returns {Object} the found context item, or undefined 
 */
SolarReg.Templates.findContextItem = function findContextItem(el) {
	var el = $(el);
	var ctx = el.data('context-item');
	if ( ctx ) {
		return ctx;
	}
	return el.parents().filter(function(i, e) {
		return !!$(e).data('context-item');
	}).first().data('context-item');
};

/**
 * Set or remove a context item on a DOM element.
 * 
 * @param {element} el the element to set or remove the context item on; a jQuery object is also allowed
 * @param {Object} [item] if provided, the context item to set; if falsy then remove any context item 
 */
SolarReg.Templates.setContextItem = function setContextItem(el, item) {
	if ( item ) {
		$(el).data('context-item', item);
	} else {
		$(el).removeData('context-item');
	}
};
