(function($){
	
	// consts
	var rad = Math.PI/180;

	// public methods
	var methods = {
		init : function(options) {
			return this.each(function() {
				var sel = $(this), d = sel.data('freetrans');
				if(d){
					_setOptions(d, options);
					_draw(sel, d);
				} else {
					_init(sel, options);
					_draw(sel, sel.data('freetrans'));
				}
			});
		},
		
		destroy : function() {
			return this.each(function() {
				_destroy($(this));
			});
		},
		
		getBounds : function() {
			if(this.length > 1) {
				$.error('Method jQuery.freetrans.getBounds can only be called on single selectors!');
			}
			
			return _getBounds(this.data('freetrans')._p.divs.controls);
		},
		
		controls: function(show) {
			return this.each(function() {
				var sel = $(this), d = sel.data('freetrans');
				if(!d) _init(sel);
			});
		},
		
		rotate: function(degrees) {
			var data = this.data('freetrans');
				data.angle = degrees;
				_draw(this,data);
		}
	};
	
	$.fn.freetrans = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else if ( typeof method === 'object' || ! method ) {
			return methods.init.apply( this, arguments );
		} else {
			$.error( 'Method ' +  method + ' does not exist on jQuery.freetrans' );
		}
		return false;
	};
	
	// private methods
	function _init(sel, options){
		
		var off = sel.offset();
		var container = sel;
		
		// generate all the controls markup
		var markup = '';
		markup += 			'<div class="ft-rotator ui-resizable-handle"></div>';

		var settings = {
			x: off.left,
			y: off.top,
			scalex: 1,
			scaley: 1, 
			angle: 0,
			'rot-origin': '50% 50%',
			_p: {
				divs: {},
				prev: {},
				wid: sel.width(),
				hgt: sel.height(),
				rad: (options && options.angle) ? options.angle * rad : 0,
				controls: true,
				dom: sel[0]
			}
		};

		var ua = navigator.userAgent;
		
		if( /webkit\//i.test(ua) ) {
			settings._p.transcss = 'WebkitTransform';
			settings._p.transorigcss = 'WebkitTransformOrigin';
		} else if (/gecko\//i.test(ua)) {
			settings._p.transcss = 'MozTransform';
			settings._p.transorigcss = 'MozTransformOrigin';
		} else if (/trident\//i.test(ua)) {
			settings._p.transcss = 'msTransform';
			settings._p.transorigcss = 'msTransformOrigin';
		} else if (/presto\//i.test(ua)) {
			settings._p.transcss = 'OTransform';
			settings._p.transorigcss = 'OTransformOrigin';
		} else {
			settings._p.transcss = 'transform';
			settings._p.transorigcss = 'transform-origin';
		}
		// append controls to container
		container.append(markup);

		// store div references (locally in function and in settings)
		var rotator = settings._p.divs.rotator = container.find('.ft-rotator');		

		sel.data('freetrans', settings);

		if(options) {
			_setOptions(sel.data('freetrans'), options);
		}


		// rotate
		rotator.bind('mousedown.freetrans', function(evt) {
			evt.stopPropagation();
			
			var data = sel.data('freetrans'),
			//cen = _getBounds(data._p.divs.controls).center,
			cen = _getBounds($(evt.currentTarget).parent().parent()).center;
			
			pressang = Math.atan2(evt.pageY - cen.y, evt.pageX - cen.x) * 180 / Math.PI;
			var rot = Number(data.angle);

			var drag = function(evt) {
				var ang = Math.atan2(evt.pageY - cen.y, evt.pageX - cen.x) * 180 / Math.PI,
				d = rot + ang - pressang;

				if(evt.shiftKey) d = (d/22.5>>0) * 22.5;

				data.angle = d;
				data._p.rad = d*rad;

				_draw(sel, data);
			};
			
			var up = function(evt) {
				$(document).unbind('mousemove.freetrans', drag);
//				$(document).unbind('mouseup.freetrans', up);
			};
			
			$(document).bind('mousemove.freetrans', drag);
			$(document).bind('mouseup.freetrans', up);
		});
		
	}
	function rotateTarget(object) {
	
	}
	
	function _destroy(sel) {
		var data = sel.data('freetrans');
		$(document).unbind('.freetrans');
		for(var el in data._p.divs) data._p.divs[el].unbind('.freetrans');
		data._p.divs.container.replaceWith(sel);
		sel.removeData('freetrans');
	}
	
	function _getBounds(sel) {
		var bnds = {};
		
			var handle = sel.children().first(),		// Te checken
			off = handle.offset(),
			hwid = handle.width() / 2,
			hhgt = handle.height() / 2;
			
			bnds.xmin = off.left + hwid;
			bnds.xmax = off.left + hwid;
			bnds.ymin = off.top + hhgt;
			bnds.ymax = off.top + hhgt;
			
			bnds.width = bnds.xmax - bnds.xmin;
			bnds.height = bnds.ymax - bnds.ymin;
			bnds.center = {'x':(bnds.xmin + (bnds.width / 2)), "y":(bnds.ymin + (bnds.height / 2))};
		
		return bnds;
	}
	
	function _setOptions(data, opts) {
		delete opts._p;

		data = $.extend(data, opts);

		if(opts.angle) data._p.rad = data.angle*rad;
		if(opts.scalex) data._p.cwid = data._p.wid * opts.scalex;
		if(opts.scaley) data._p.chgt = data._p.hgt * opts.scaley;
	}

	function _getRotationPoint(sel) {
		var data = sel.data('freetrans'), 
		ror = data['rot-origin'], 
		pt = {'x':0, "y":0 };
		
		if(!ror || ror == "50% 50%") return pt;
		
		var arr = ror.split(' '), l = arr.length;
		
		if(!l) return pt;
		
		var val = parseInt(arr[0]), 
		per = arr[0].indexOf('%') > -1,
		dim = data._p.cwid;

		pt.x = ((per) ? val/100*dim : val) - dim/2;

		if(l==1)  pt.y = pt.x;
		else {
			val = arr[1];
			per = val.indexOf('%') > -1;
			val = parseInt(val);	
			dim = data._p.chgt;
			pt.y = ((per) ? val/100*dim : val) - dim/2;		
		}

		return pt;
	}
	
	function _draw(sel, data) {		
		if(!data) return;
		
		var el;
		el = data._p.dom;

		// we need to rotate
		if(data.angle != data._p.prev.angle){
			if(data.angle){ 
				data._p.prev.angle = data.angle;
				el.style[data._p.transcss] = "rotate("+data.angle+"deg)";
			}
		}
	}
})(jQuery);