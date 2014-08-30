/* Copyright 2012 Mozilla Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @ignore(PDFJS.*)
 * @ignore(Promise*)
 * @ignore(jQuery*)
 * @ignore(TextLayerBuilder*)
 * @lint ignoreDeprecated(alert,eval)
 * @ignore(alert*)
 */
qx.Class.define("ms123.pdf.PageView", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (pdfView, container, id, scale, navigateTo, defaultViewport) {
		this.base(arguments);
		this.pdfView = pdfView;
		console.log("pdfView:" + pdfView + "/" + pdfView.hotspots);
		this.id = id;

		this.rotation = 0;
		this.scale = scale || 1.0;
		this.viewport = defaultViewport;
		this.pdfPageRotate = defaultViewport.rotation;

		this.renderingState = ms123.pdf.PDFView.RenderingStates.INITIAL;
		this.resume = null;

		this.textLayer = null;

		this.zoomLayer = null;

		this.annotationLayer = null;

		var anchor = document.createElement('a');
		anchor.name = '' + this.id;

		var pageDiv = this.el = document.createElement('div');
		pageDiv.id = 'pageContainer' + this.id;
		pageDiv.className = 'pdf-page';
		pageDiv.style.width = Math.floor(this.viewport.width) + 'px';
		pageDiv.style.height = Math.floor(this.viewport.height) + 'px';
		this.pageDiv = pageDiv;

		container.appendChild(anchor);
		container.appendChild(pageDiv);


		Object.defineProperty(this, 'width', {
			get: function PageView_getWidth() {
				return this.viewport.width;
			},
			enumerable: true
		});

		Object.defineProperty(this, 'height', {
			get: function PageView_getHeight() {
				return this.viewport.height;
			},
			enumerable: true
		});

		var self = this;
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		setPdfPage: function (pdfPage) {
			this.pdfPage = pdfPage;
			this.pdfPageRotate = pdfPage.rotate;
			var totalRotation = (this.rotation + this.pdfPageRotate) % 360;
			this.viewport = pdfPage.getViewport(this.scale * ms123.pdf.PDFView.CSS_UNITS, totalRotation);
			this.stats = pdfPage.stats;
			this.reset();
		},
		destroy: function () {
			this.zoomLayer = null;
			this.reset();
			if (this.pdfPage) {
				this.pdfPage.destroy();
			}
		},

		reset: function () {
			if (this.renderTask) {
				this.renderTask.cancel();
			}
			this.resume = null;
			this.renderingState = ms123.pdf.PDFView.RenderingStates.INITIAL;

			var pageDiv = this.pageDiv;
			pageDiv.style.width = Math.floor(this.viewport.width) + 'px';
			pageDiv.style.height = Math.floor(this.viewport.height) + 'px';

			var childNodes = pageDiv.childNodes;
			for (var i = pageDiv.childNodes.length - 1; i >= 0; i--) {
				var node = childNodes[i];
				if (this.zoomLayer && this.zoomLayer === node) {
					continue;
				}
				pageDiv.removeChild(node);
			}
			pageDiv.removeAttribute('data-loaded');

			this.annotationLayer = null;
			this.hotspotLayer = null;

			delete this.canvas;

			this.loadingIconDiv = document.createElement('div');
			this.loadingIconDiv.className = 'loadingIcon';
			pageDiv.appendChild(this.loadingIconDiv);
		},
		update: function (scale, rotation) {
			this.scale = scale || this.scale;

			if (typeof rotation !== 'undefined') {
				this.rotation = rotation;
			}

			var totalRotation = (this.rotation + this.pdfPageRotate) % 360;
			this.viewport = this.viewport.clone({
				scale: this.scale * ms123.pdf.PDFView.CSS_UNITS,
				rotation: totalRotation
			});

			if (ms123.pdf.PDFView.USE_ONLY_CSS_ZOOM && this.canvas) {
				this.cssTransform(this.canvas);
				return;
			} else if (this.canvas && !this.zoomLayer) {
				this.zoomLayer = this.canvas.parentNode;
				this.zoomLayer.style.position = 'absolute';
			}
			if (this.zoomLayer) {
				this.cssTransform(this.zoomLayer.firstChild);
			}
			this.reset();
		},

		cssTransform: function (canvas) {
			// Scale canvas, canvas wrapper, and page container.
			var width = this.viewport.width;
			var height = this.viewport.height;
			var pageDiv = this.pageDiv;
			canvas.style.width = canvas.parentNode.style.width = pageDiv.style.width = Math.floor(width) + 'px';
			canvas.style.height = canvas.parentNode.style.height = pageDiv.style.height = Math.floor(height) + 'px';
			// The canvas may have been originally rotated, so rotate relative to that.
			var relativeRotation = this.viewport.rotation - canvas._viewport.rotation;
			var absRotation = Math.abs(relativeRotation);
			var scaleX = 1,
				scaleY = 1;
			if (absRotation === 90 || absRotation === 270) {
				// Scale x and y because of the rotation.
				scaleX = height / width;
				scaleY = width / height;
			}
			var cssTransform = 'rotate(' + relativeRotation + 'deg) ' + 'scale(' + scaleX + ',' + scaleY + ')';
			var style = ms123.pdf.Style;
			style.setProp('transform', canvas, cssTransform);
			//	style.setProp('transition', canvas, "1s ease-in");
			if (this.textLayer) {
				// Rotating the text layer is more complicated since the divs inside the the text layer are rotated.
				// This could probably be simplified by drawing the text layer in one orientation then rotating overall.
				var textRelativeRotation = this.viewport.rotation - this.textLayer.viewport.rotation;
				var textAbsRotation = Math.abs(textRelativeRotation);
				var scale = (width / canvas.width);
				if (textAbsRotation === 90 || textAbsRotation === 270) {
					scale = width / canvas.height;
				}
				var textLayerDiv = this.textLayer.textLayerDiv;
				var transX, transY;
				switch (textAbsRotation) {
				case 0:
					transX = transY = 0;
					break;
				case 90:
					transX = 0;
					transY = '-' + textLayerDiv.style.height;
					break;
				case 180:
					transX = '-' + textLayerDiv.style.width;
					transY = '-' + textLayerDiv.style.height;
					break;
				case 270:
					transX = '-' + textLayerDiv.style.width;
					transY = 0;
					break;
				default:
					console.error('Bad rotation value.');
					break;
				}
				style.setProp('transform', textLayerDiv, 'rotate(' + textAbsRotation + 'deg) ' + 'scale(' + scale + ', ' + scale + ') ' + 'translate(' + transX + ', ' + transY + ')');
				style.setProp('transformOrigin', textLayerDiv, '0% 0%');
			}

			if (ms123.pdf.PDFView.USE_ONLY_CSS_ZOOM && this.annotationLayer) {
				this.setupAnnotations(pageDiv, this.pdfPage, this.viewport);
			}
		},
		draw: function (callback) {
			var pageDiv = this.pageDiv;
			var pdfPage = this.pdfPage;
			if (this.pagePdfPromise) {
				return;
			}
			if (!pdfPage) {
				var promise = this.pdfView.getPage(this.id);
				promise.then(function (pdfPage) {
					delete this.pagePdfPromise;
					this.setPdfPage(pdfPage);
					this.draw(callback);
				}.bind(this));
				this.pagePdfPromise = promise;
				return;
			}

			if (this.renderingState !== ms123.pdf.PDFView.RenderingStates.INITIAL) {
				console.error('Must be in new state before drawing');
			}

			this.renderingState = ms123.pdf.PDFView.RenderingStates.RUNNING;

			var viewport = this.viewport;
			// Wrap the canvas so if it has a css transform for highdpi the overflow will be hidden in FF.
			var canvasWrapper = document.createElement('div');
			canvasWrapper.style.width = pageDiv.style.width;
			canvasWrapper.style.height = pageDiv.style.height;
			canvasWrapper.classList.add('pdf-canvasWrapper');

			var canvas = document.createElement('canvas');
			//canvas.id = 'page' + this.id;
			canvas.classList.add('pdf-canvas');
			canvasWrapper.appendChild(canvas);
			pageDiv.appendChild(canvasWrapper);
			this.canvas = canvas;

			var scale = this.scale;
			var ctx = canvas.getContext('2d');
			var outputScale = this.getOutputScale(ctx);

			if (ms123.pdf.PDFView.USE_ONLY_CSS_ZOOM) {
				var actualSizeViewport = viewport.clone({
					scale: ms123.pdf.PDFView.CSS_UNITS
				});
				// Use a scale that will make the canvas be the original intended size  of the page.
				outputScale.sx *= actualSizeViewport.width / viewport.width;
				outputScale.sy *= actualSizeViewport.height / viewport.height;
				outputScale.scaled = true;
			}

			canvas.width = (Math.floor(viewport.width) * outputScale.sx) | 0;
			canvas.height = (Math.floor(viewport.height) * outputScale.sy) | 0;
			canvas.style.width = Math.floor(viewport.width) + 'px';
			canvas.style.height = Math.floor(viewport.height) + 'px';
			// Add the viewport so it's known what it was originally drawn with.
			canvas._viewport = viewport;

			var textLayerDiv = null;
			if (false /*!PDFJS.disableTextLayer*/ ) {
				textLayerDiv = document.createElement('div');
				textLayerDiv.className = 'textLayer';
				textLayerDiv.style.width = canvas.style.width + 'px';
				textLayerDiv.style.height = canvas.style.height + 'px';
				pageDiv.appendChild(textLayerDiv);
			}
			var textLayer = this.textLayer = textLayerDiv ? new TextLayerBuilder({
				textLayerDiv: textLayerDiv,
				pageIndex: this.id - 1,
				lastScrollSource: this.pdfView,
				viewport: this.viewport,
				isViewerInPresentationMode: false //PresentationMode.active
			}) : null;
			ctx._scaleX = outputScale.sx;
			ctx._scaleY = outputScale.sy;
			if (outputScale.scaled) {
				ctx.scale(outputScale.sx, outputScale.sy);
			}

			// Rendering area
			var self = this;

			function pageViewDrawCallback(error) {
				// The renderTask may have been replaced by a new one, so only remove the
				// reference to the renderTask if it matches the one that is triggering this callback.
				if (renderTask === self.renderTask) {
					self.renderTask = null;
				}

				if (error === 'cancelled') {
					return;
				}

				self.renderingState = ms123.pdf.PDFView.RenderingStates.FINISHED;

				if (self.loadingIconDiv) {
					pageDiv.removeChild(self.loadingIconDiv);
					delete self.loadingIconDiv;
				}

				if (self.zoomLayer) {
					pageDiv.removeChild(self.zoomLayer);
					self.zoomLayer = null;
				}

				if (error) {
					this.pdfView.error(this.tr('pdfview.rendering_error'), error);
				}

				self.stats = pdfPage.stats;
				self.updateStats();
				if (self.onAfterDraw) {
					self.onAfterDraw();
				}

				//cache.push(self);
				var event = document.createEvent('CustomEvent');
				event.initCustomEvent('pagerender', true, true, {
					pageNumber: pdfPage.pageNumber
				});
				pageDiv.dispatchEvent(event);
				callback();
			}

			var renderContext = {
				canvasContext: ctx,
				viewport: this.viewport,
				textLayer: textLayer,
				continueCallback: function pdfViewcContinueCallback(cont) {
					if (self.pdfView.highestPriorityPage !== 'page' + self.id) {
						self.renderingState = ms123.pdf.PDFView.RenderingStates.PAUSED;
						self.resume = function resumeCallback() {
							self.renderingState = ms123.pdf.PDFView.RenderingStates.RUNNING;
							cont();
						};
						return;
					}
					cont();
				}
			};
			var renderTask = this.renderTask = this.pdfPage.render(renderContext);
			this.renderTask.promise.then(

			function pdfPageRenderCallback() {
				pageViewDrawCallback(null);
			}, function pdfPageRenderError(error) {
				pageViewDrawCallback(error);
			});

			if (textLayer) {
				this.getTextContent().then(

				function textContentResolved(textContent) {
					textLayer.setTextContent(textContent);
				});
			}

			this.setupHotspots(this.pdfView.hotspots, pageDiv, pdfPage, this.viewport);
			this.setupAnnotations(pageDiv, pdfPage, this.viewport);
			pageDiv.setAttribute('data-loaded', true);
		},
		scrollIntoView: function (dest) {
			//console.error("scrollIntoView:" + JSON.stringify(dest));
/*if (PresentationMode.active) {
							if (this.pdfView.page !== this.id) {
								// Avoid breaking this.pdfView.getVisiblePages in presentation mode.
								this.pdfView.page = this.id;
								return;
							}
							dest = null;
							this.pdfView.setScale(this.pdfView.currentScaleValue, true, true);
						}*/
			if (!dest) {
				this.scrollIntoView2(this.pageDiv);
				return;
			}

			var x = 0,
				y = 0;
			var xDelta = 0,
				yDelta = 0;
			var width = 0,
				height = 0,
				widthScale, heightScale;
			var changeOrientation = !! (this.rotation % 180);
			var pageWidth = (changeOrientation ? this.height : this.width) / this.scale / ms123.pdf.PDFView.CSS_UNITS;
			var pageHeight = (changeOrientation ? this.width : this.height) / this.scale / ms123.pdf.PDFView.CSS_UNITS;
			var scale = 0;
			switch (dest[1].name) {
			case 'XYZ':
				x = dest[2];
				y = dest[3];
				xDelta = dest[4] || 0;
				yDelta = dest[5] || 0;
				scale = dest[6];
				x = x !== null ? x : 0;
				y = y !== null ? y : pageHeight;
				break;
			case 'Fit':
			case 'FitB':
				scale = 'page-fit';
				break;
			case 'FitH':
			case 'FitBH':
				y = dest[2];
				scale = 'page-width';
				break;
			case 'FitV':
			case 'FitBV':
				x = dest[2];
				width = pageWidth;
				height = pageHeight;
				scale = 'page-height';
				break;
			case 'FitR':
				x = dest[2];
				y = dest[3];
				width = dest[4] - x;
				height = dest[5] - y;
				widthScale = (this.pdfView.container.clientWidth - ms123.pdf.PDFView.SCROLLBAR_PADDING) / width / ms123.pdf.PDFView.CSS_UNITS;
				heightScale = (this.pdfView.container.clientHeight - ms123.pdf.PDFView.SCROLLBAR_PADDING) / height / ms123.pdf.PDFView.CSS_UNITS;
				scale = Math.min(Math.abs(widthScale), Math.abs(heightScale));
				break;
			default:
				return;
			}

			if (scale && scale !== this.pdfView.currentScale) {
				this.pdfView.setScale(scale, true, true);
			} else if (this.pdfView.currentScale === ms123.pdf.PDFView.UNKNOWN_SCALE) {
				this.pdfView.setScale(ms123.pdf.PDFView.DEFAULT_SCALE, true, true);
			}

			if (scale === 'page-fit' && !dest[4]) {
				this.scrollIntoView2(this.pageDiv);
				return;
			}

			var boundingRect = [
			this.viewport.convertToViewportPoint(x, y), this.viewport.convertToViewportPoint(x + width, y + height)];
			var left = Math.min(boundingRect[0][0], boundingRect[1][0]) + xDelta;
			var top = Math.min(boundingRect[0][1], boundingRect[1][1]) + yDelta;

			this.scrollIntoView2(this.pageDiv, {
				left: left,
				top: top
			});
		},
		getPagePoint: function (x, y) {
			return this.viewport.convertToPdfPoint(x, y);
		},
		getTextContent: function () {
			return this.pdfView.getPage(this.id).then(function (pdfPage) {
				return pdfPage.getTextContent();
			});
		},
		selectHotspot: function (href, intern) {
			if (this.internalHotspotEvent === true) return;
			this.selectedHotspots = [];
			console.log("selectHotspot:" + href + "/" + intern);
			var elems = this.pageDiv.getElementsByClassName("hotspot");
			var firstSelected = null;
			for (var i = 0; i < elems.length; i++) {
				var e = elems[i];
				jQuery(e).removeClass("selected");
				if (e.href == "#" + href) {
					jQuery(e).addClass("selected");
					this.selectedHotspots.push(href);
					if (!firstSelected) {
						firstSelected = e;
					}
				}
			}
			if (!intern && firstSelected) {
				var e = firstSelected;
				var offsetY = e.offsetTop + e.clientTop;
				var offsetX = e.offsetLeft + e.clientLeft;
				var xy = null;
				if (this.viewport.rotation == 90) {
					var viewport = this.viewport.clone({
						dontFlip: true
					});
					xy = viewport.convertToViewportPoint(offsetX + 0, offsetY + 0);
				} else {
					xy = this.viewport.convertToViewportPoint(offsetX + 0, offsetY + 0);
				}
				var bounds = this.pdfView.getBounds();
				this.pdfView.contentElement.scrollLeft = xy[0] - bounds.width / 2;
				this.pdfView.contentElement.scrollTop = xy[1] - bounds.height / 2;
			}
		},
		setupHotspots: function (hotspots, pageDiv, pdfPage, viewport) {
			var dontFlip = viewport.rotation == 90;
			if (hotspots == null) return;
			viewport = viewport.clone({
				dontFlip: dontFlip
			});
			var style = ms123.pdf.Style;
			if (this.hotspotLayer) {
				pageDiv.removeChild(this.hotspotLayer);
				this.hotspotLayer = null;
			}
			var hsList = hotspots;
			for (var i = 0; i < hsList.length; i++) {
				var hs = hsList[i];
				var rect = hs.coords.split(",");
				if (dontFlip) {
					var f = .8;
					var x = rect[0];
					rect[0] = rect[1] * f;
					rect[1] = x * f;
					var x = rect[2];
					rect[2] = rect[3] * f;
					rect[3] = x * f;
				} else {
					var f = 1.0;
					rect[0] = rect[0] * f;
					rect[1] = rect[1] * f;
					rect[2] = rect[2] * f;
					rect[3] = rect[3] * f;
				}

				var element = this.createEmptyContainer('section', rect, 1);
				var view = pdfPage.view;
				rect = PDFJS.Util.normalizeRect([rect[0], view[3] - rect[1] + view[1], rect[2], view[3] - rect[3] + view[1]]);
				element.style.left = (rect[0] + 2) + 'px';
				element.style.top = rect[1] + 'px';
				element.style.position = 'absolute';
				//	element.style.border = '1px solid blue';
				element.style.opacity = 0.20;
				element.className = 'hotspot';
				element.style.zIndex = 99;
				if (this.selectedHotspots && this.selectedHotspots.indexOf(hs.href) >= 0) {
					jQuery(element).addClass("selected");
				}

				element.href = '#' + hs.href;
				q(element).on("click", function (e) {
					e.preventDefault();
					var target = e.target || e.srcElement;
					var href = target.href.substring(1);
					var data = {
						page: this.id,
						href: href
					}
					this.selectHotspot(href, true);
					this.internalHotspotEvent = true;
					this.pdfView.fireDataEvent("hotspot", data, null);
					this.internalHotspotEvent = false;
				}, this);

				var transform = viewport.transform;
				var transformStr = 'matrix(' + transform.join(',') + ')';
				style.setProp('transform', element, transformStr);
				var transformOriginStr = -rect[0] + 'px ' + -rect[1] + 'px';
				style.setProp('transformOrigin', element, transformOriginStr);

				if (!this.hotspotLayer) {
					var hotspotLayerDiv = document.createElement('div');
					hotspotLayerDiv.className = 'hotspotLayer';
					pageDiv.appendChild(hotspotLayerDiv);
					this.hotspotLayer = hotspotLayerDiv;
				}
				this.hotspotLayer.appendChild(element);
			}
		},
		createEmptyContainer: function (tagName, rect, borderWidth) {
			var bWidth = borderWidth || 0;

			var element = document.createElement(tagName);
			element.style.borderWidth = bWidth + 'px';
			var width = rect[2] - rect[0]; //+ 2 * bWidth;
			var height = rect[3] - rect[1]; // + 2 * bWidth;
			element.style.width = width + 'px';
			element.style.height = height + 'px';
			return element;
		},
		setupAnnotations: function (pageDiv, pdfPage, viewport) {

			function bindLink(link, dest) {
				link.href = this.pdfView.getDestinationHash(dest);
				link.onclick = function pageViewSetupLinksOnclick() {
					if (dest) {
						this.pdfView.navigateTo(dest);
					}
					return false;
				};
				if (dest) {
					link.className = 'internalLink';
				}
			}

			function bindNamedAction(link, action) {
				link.href = this.pdfView.getAnchorUrl('');
				link.onclick = function pageViewSetupNamedActionOnClick() {
					// See PDF reference, table 8.45 - Named action
					switch (action) {
					case 'GoToPage':
						document.getElementById('pageNumber').focus();
						break;

					case 'GoBack':
						//PDFHistory.back();
						break;

					case 'GoForward':
						//PDFHistory.forward();
						break;

					case 'Find':
						//if (!this.pdfView.supportsIntegratedFind) {
						//		PDFFindBar.toggle();
						//	}
						break;

					case 'NextPage':
						//	this.pdfView.setPageNumber(this.pdfView.getPageNumber()+1);
						break;

					case 'PrevPage':
						//	this.pdfView.setPageNumber(this.pdfView.getPageNumber()-1);
						break;

					case 'LastPage':
						//	this.pdfView.setPageNumber(this.pdfView.pages.length);
						break;

					case 'FirstPage':
						//	this.pdfView.setPageNumber(1);
						break;

					default:
						break; // No action according to spec
					}
					return false;
				};
				link.className = 'internalLink';
			}

			var self = this;
			pdfPage.getAnnotations().then(function (annotationsData) {
				if (self.annotationLayer) {
					// If an annotationLayer already exists, delete it to avoid creating
					// duplicate annotations when rapidly re-zooming the document.
					pageDiv.removeChild(self.annotationLayer);
					self.annotationLayer = null;
				}
				viewport = viewport.clone({
					dontFlip: true
				});
				var style = ms123.pdf.Style;
				for (var i = 0; i < annotationsData.length; i++) {
					var data = annotationsData[i];
					var annotation = PDFJS.Annotation.fromData(data);
					if (!annotation || !annotation.hasHtml()) {
						continue;
					}

					var element = annotation.getHtmlElement(pdfPage.commonObjs);

					data = annotation.getData();
					var rect = data.rect;
					var view = pdfPage.view;
					rect = PDFJS.Util.normalizeRect([
					rect[0], view[3] - rect[1] + view[1], rect[2], view[3] - rect[3] + view[1]]);
					element.style.left = rect[0] + 'px';
					element.style.top = rect[1] + 'px';
					element.style.position = 'absolute';

					var transform = viewport.transform;
					var transformStr = 'matrix(' + transform.join(',') + ')';
					style.setProp('transform', element, transformStr);
					var transformOriginStr = -rect[0] + 'px ' + -rect[1] + 'px';
					style.setProp('transformOrigin', element, transformOriginStr);

					if (data.subtype === 'Link' && !data.url) {
						var link = element.getElementsByTagName('a')[0];
						if (link) {
							if (data.action) {
								bindNamedAction(link, data.action);
							} else {
								bindLink(link, ('dest' in data) ? data.dest : null);
							}
						}
					}

					if (!self.annotationLayer) {
						var annotationLayerDiv = document.createElement('div');
						annotationLayerDiv.className = 'annotationLayer';
						pageDiv.appendChild(annotationLayerDiv);
						self.annotationLayer = annotationLayerDiv;
					}
					self.annotationLayer.appendChild(element);
				}
			});
		},
		beforePrint: function () {
			var pdfPage = this.pdfPage;

			var viewport = pdfPage.getViewport(1);
			// Use the same hack we use for high dpi displays for printing to get better
			// output until bug 811002 is fixed in FF.
			var PRINT_OUTPUT_SCALE = 2;
			var canvas = document.createElement('canvas');
			canvas.width = Math.floor(viewport.width) * PRINT_OUTPUT_SCALE;
			canvas.height = Math.floor(viewport.height) * PRINT_OUTPUT_SCALE;
			canvas.style.width = (PRINT_OUTPUT_SCALE * viewport.width) + 'pt';
			canvas.style.height = (PRINT_OUTPUT_SCALE * viewport.height) + 'pt';
			var cssScale = 'scale(' + (1 / PRINT_OUTPUT_SCALE) + ', ' + (1 / PRINT_OUTPUT_SCALE) + ')';

			var style = ms123.pdf.Style;
			style.setProp('transform', canvas, cssScale);
			style.setProp('transformOrigin', canvas, '0% 0%');

			var printContainer = document.getElementById('printContainer');
			var canvasWrapper = document.createElement('div');
			canvasWrapper.style.width = viewport.width + 'pt';
			canvasWrapper.style.height = viewport.height + 'pt';
			canvasWrapper.appendChild(canvas);
			printContainer.appendChild(canvasWrapper);

			var self = this;
			canvas.mozPrintCallback = function (obj) {
				var ctx = obj.context;

				ctx.save();
				ctx.fillStyle = 'rgb(255, 255, 255)';
				ctx.fillRect(0, 0, canvas.width, canvas.height);
				ctx.restore();
				ctx.scale(PRINT_OUTPUT_SCALE, PRINT_OUTPUT_SCALE);

				var renderContext = {
					canvasContext: ctx,
					viewport: viewport,
					intent: 'print'
				};

				pdfPage.render(renderContext).promise.then(function () {
					// Tell the printEngine that rendering this canvas/page has finished.
					obj.done();
				}, function (error) {
					console.error(error);
					// Tell the printEngine that rendering this canvas/page has failed.
					// This will make the print proces stop.
					if ('abort' in obj) {
						obj.abort();
					} else {
						obj.done();
					}
				});
			};
		},

		getOutputScale: function (ctx) {
			var devicePixelRatio = window.devicePixelRatio || 1;
			var backingStoreRatio = ctx.webkitBackingStorePixelRatio || ctx.mozBackingStorePixelRatio || ctx.msBackingStorePixelRatio || ctx.oBackingStorePixelRatio || ctx.backingStorePixelRatio || 1;
			var pixelRatio = devicePixelRatio / backingStoreRatio;
			return {
				sx: pixelRatio,
				sy: pixelRatio,
				scaled: pixelRatio != 1
			};
		},

		scrollIntoView2: function (element, spot) {
			var parent = element.offsetParent;
			var offsetY = element.offsetTop + element.clientTop;
			var offsetX = element.offsetLeft + element.clientLeft;
			if (!parent) {
				console.error('offsetParent is not set -- cannot scroll');
				return;
			}
			while (parent.clientHeight === parent.scrollHeight) {
				if (parent.dataset._scaleY) {
					offsetY /= parent.dataset._scaleY;
					offsetX /= parent.dataset._scaleX;
				}
				offsetY += parent.offsetTop;
				offsetX += parent.offsetLeft;
				parent = parent.offsetParent;
				if (!parent) {
					return; // no need to scroll
				}
			}
			if (spot) {
				if (spot.top !== undefined) {
					offsetY += spot.top;
				}
				if (spot.left !== undefined) {
					offsetX += spot.left;
				}
			}
			parent.scrollLeft = offsetX;
			parent.scrollTop = offsetY;
		},
		updateStats: function () {
			if (!this.stats) {
				return;
			}

		}

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
