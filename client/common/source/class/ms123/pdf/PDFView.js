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
 * @lint ignoreDeprecated(alert,eval)
 * @ignore(alert*)
 */
qx.Class.define('ms123.pdf.PDFView', {
	extend: qx.ui.embed.Html,

	construct: function (context) {
		this.base(arguments);

		//this.setOverflowX("auto");
		//this.setOverflowY("auto");
		this.context = context;
		PDFJS.workerSrc = "legacy/js/pdf.worker.js.gz";
		this.currentScale = ms123.pdf.PDFView.UNKNOWN_SCALE;
		this.pages = [];
		this.thumbnails = [];
		this.addListenerOnce('appear', function () {
			this.initialize(context.controlContainer);
			this.open(context.url, context.hotspots, context.scale);
		}, this);
	},

	properties: {},
	events: {
		"change": "qx.event.type.Data",
		"hotspot": "qx.event.type.Data"
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		DEFAULT_SCALE: 'auto',
		DEFAULT_SCALE_DELTA: 1.1,
		UNKNOWN_SCALE: 0,
		CACHE_SIZE: 20,
		CSS_UNITS: 96.0 / 72.0,
		SCROLLBAR_PADDING: 40,
		VERTICAL_PADDING: 5,
		MAX_AUTO_SCALE: 1.25,
		MIN_SCALE: 0.25,
		MAX_SCALE: 8.0,
		SCALE_SELECT_CONTAINER_PADDING: 8,
		SCALE_SELECT_PADDING: 22,
		USE_ONLY_CSS_ZOOM: false,
		CLEANUP_TIMEOUT: 30000,
		IGNORE_CURRENT_POSITION_ON_ZOOM: false,
		MOUSE_SCROLL_COOLDOWN_TIME: 50,
		PAGE_FLIP_THRESHOLD: 120,
		RenderingStates: {
			INITIAL: 0,
			RUNNING: 1,
			PAUSED: 2,
			FINISHED: 3
		},
		FindStates: {
			FIND_FOUND: 0,
			FIND_NOTFOUND: 1,
			FIND_WRAPPED: 2,
			FIND_PENDING: 3
		}
	},

	members: {
		currentScaleValue: null,
		contentElement: null,
		thumbnailContainer: null,
		initialized: false,
		pdfDocument: null,
		pageViewScroll: null,
		pageRotation: 0,
		mouseScrollTimeStamp: 0,
		mouseScrollDelta: 0,
		previousPageNumber: 1,
		idleTimeout: null,
		currentPosition: null,

		initialize: function pdfViewInitialize(controlContainer) {
			var self = this;
			var contentElement = self.getContentElement().getDomElement();
			this.viewerElement = document.createElement('div');
			contentElement.appendChild(this.viewerElement);
			this.contentElement = contentElement;

			this.pageViewScroll = {};
			this.watchScroll(contentElement, this.pageViewScroll, this.updateViewarea.bind(this));

			this.thumbnailViewScroll = {};
			this.thumbnailContainer = document.createElement('div');
			this.watchScroll(this.thumbnailContainer, this.thumbnailViewScroll, this.renderHighestPriority.bind(this));
			var c = controlContainer.getContentElement().getDomElement();
			c.appendChild(this.thumbnailContainer);
			this.page = 1;

			jQuery(contentElement).kinetic({
				cursor: 'pointer',
				slowdown: 0.95
			});
			this.initialized = true;

			function handleMouseWheel(evt) {
				var MOUSE_WHEEL_DELTA_FACTOR = 40;
				var ticks = (evt.type === 'DOMMouseScroll') ? -evt.detail : evt.wheelDelta / MOUSE_WHEEL_DELTA_FACTOR;

				var box = self.getContentLocation("box");
				var oldX = evt.clientX - box.left
				var oldY = evt.clientY - box.top
				var oldPoint = {x:oldX,y:oldY};
				var newScale = self.calcNewScale(ticks);
				var newPoint = self.getNewPoint(ticks, oldPoint,newScale);
				var positionDelta = self.getPositionDelta(oldPoint, newPoint);
				self.currentPoint = newPoint;

				if (true) {
					evt.preventDefault();
					self.setScale(newScale, false, positionDelta);
				} else if (false /*PresentationMode.active*/ ) {
					this.mouseScroll(ticks * MOUSE_WHEEL_DELTA_FACTOR);
				}
			}

			contentElement.addEventListener('DOMMouseScroll', handleMouseWheel);
			contentElement.addEventListener('mousewheel', handleMouseWheel);
			contentElement.addEventListener('scaleChange', function (evt) {
				self.updateViewarea();
			}, true);

			self.addListener('resize', function (evt) {
				if (self.initialized) {
					self.setScale(self.currentScale);
					self.updateViewarea();
				}
			});

		},

		getPositionDelta:function(oldPoint, newPoint ){
			var xDiff = newPoint.x - oldPoint.x;
			var yDiff = newPoint.y - oldPoint.y;
			var positionDelta = {
				x: xDiff,
				y: yDiff
			}
			return positionDelta;
		},
		getNewPoint:function(ticks, oldPoint, newScale ){
			var oldScale = this.currentScale;
			var newX = (oldPoint.x / oldScale) * newScale;
			var newY = (oldPoint.y / oldScale) * newScale;
			var point = {
				x: newX,
				y: newY
			}
			return point;
		},

		getPage: function (n) {
			return this.pdfDocument.getPage(n);
		},

		watchScroll: function pdfViewWatchScroll(viewAreaElement, state, callback) {
			state.down = true;
			state.lastY = viewAreaElement.scrollTop;
			viewAreaElement.addEventListener('scroll', function webViewerScroll(evt) {
				var currentY = viewAreaElement.scrollTop;
				var lastY = state.lastY;
				if (currentY > lastY) {
					state.down = true;
				} else if (currentY < lastY) {
					state.down = false;
				}
				state.lastY = currentY;
				callback();
			}, true);
		},

		_setScaleUpdatePages: function pdfView_setScaleUpdatePages(newScale, newValue, noScroll, positionDelta) {
			this.currentScaleValue = newValue;
			if (newScale === this.currentScale) {
				return;
			}
			for (var i = 0, ii = this.pages.length; i < ii; i++) {
				this.pages[i].update(newScale);
			}
			this.currentScale = newScale;

			if (!noScroll) {
				positionDelta = positionDelta || {};
				var page = this.getCurrentPage();
				if (this.currentPosition && !ms123.pdf.PDFView.IGNORE_CURRENT_POSITION_ON_ZOOM) {
					page = this.currentPosition.page;
					var dest = [null,
					{
						name: 'XYZ'
					},
					this.currentPosition.left, this.currentPosition.top, positionDelta.x, positionDelta.y, null];
				}
				this.pages[page - 1].scrollIntoView(dest);
			}
			var event = document.createEvent('UIEvents');
			event.initUIEvent('scaleChange', false, false, document.defaultView, 0);
			event.scale = newScale;
			this.contentElement.dispatchEvent(event);
		},

		setScale: function (value, noScroll, positionDelta) {
			var scale = parseFloat(value);

			if (scale > 0) {
				this._setScaleUpdatePages(scale, value, noScroll, positionDelta);
			} else {
				var currentPage = this.pages[this.page - 1];
				if (!currentPage) {
					return;
				}
				var hPadding = /*PresentationMode.active ? 0 :*/
				ms123.pdf.PDFView.SCROLLBAR_PADDING;
				var vPadding = /*PresentationMode.active ? 0 :*/
				ms123.pdf.PDFView.VERTICAL_PADDING;
				var pageWidthScale = (this.contentElement.clientWidth - hPadding) / currentPage.width * currentPage.scale;
				var pageHeightScale = (this.contentElement.clientHeight - vPadding) / currentPage.height * currentPage.scale;
				switch (value) {
				case 'page-actual':
					scale = 1;
					break;
				case 'page-width':
					scale = pageWidthScale;
					break;
				case 'page-height':
					scale = pageHeightScale;
					break;
				case 'page-fit':
					scale = Math.min(pageWidthScale, pageHeightScale);
					break;
				case 'auto':
					scale = Math.min(ms123.pdf.PDFView.MAX_AUTO_SCALE, pageWidthScale);
					break;
				default:
					console.error('pdfViewSetScale: \'' + value + '\' is an unknown zoom value.');
					return;
				}
				this._setScaleUpdatePages(scale, value, noScroll, positionDelta);
				this.currentScale = scale;
				this.currentScaleValue = value;

				//selectScaleOption(value);
			}
		},

		zoomIn: function (ticks) {
			ticks = ticks || 1;
			var newScale = this.calcNewScale(ticks);
			var newPoint = this.getNewPoint(ticks,this.currentPoint,newScale);
			this.setScale(newScale,false, this.getPositionDelta(this.currentPoint, newPoint));
			this.currentPoint = newPoint;
		},

		zoomOut: function (ticks) {
			ticks = ticks || -1;
			var newScale = this.calcNewScale(ticks);
			var newPoint = this.getNewPoint(ticks,this.currentPoint,newScale);
			this.setScale(newScale, false, this.getPositionDelta(this.currentPoint, newPoint));
			this.currentPoint = newPoint;
		},

		calcNewScale: function (ticks) {
			var newScale = this.currentScale;
			if (ticks >= 0) {
				do {
					newScale = (newScale * ms123.pdf.PDFView.DEFAULT_SCALE_DELTA).toFixed(2);
					newScale = Math.ceil(newScale * 10) / 10;
					newScale = Math.min(ms123.pdf.PDFView.MAX_SCALE, newScale);
				} while (--ticks && newScale < ms123.pdf.PDFView.MAX_SCALE);
			} else {
				ticks = Math.abs(ticks);
				do {
					newScale = (newScale / ms123.pdf.PDFView.DEFAULT_SCALE_DELTA).toFixed(2);
					newScale = Math.floor(newScale * 10) / 10;
					newScale = Math.max(ms123.pdf.PDFView.MIN_SCALE, newScale);
				} while (--ticks && newScale > ms123.pdf.PDFView.MIN_SCALE);
			}
			return newScale;
		},


		setCurrentPage: function (val) {
			var pages = this.pages;
			//var event = document.createEvent('UIEvents');
			//event.initUIEvent('pagechange', false, false, window, 0);
			if (!(0 < val && val <= pages.length)) {
				this.previousPageNumber = val;
				//event.pageNumber = this.page;
				//window.dispatchEvent(event);
				return;
			}

			pages[val - 1].updateStats();
			this.previousPageNumber = this._currentPageNumber;
			this._currentPageNumber = val;
			//event.pageNumber = val;
			//window.dispatchEvent(event);
			// checking if the this.page was called from the updateViewarea function:
			// avoiding the creation of two "set page" method (internal and public)
			if (this.updateViewareaInProgress) {
				return;
			}
			// Avoid scrolling the first page during loading
			if (this.loading && val === 1) {
				return;
			}
			pages[val - 1].scrollIntoView();
		},

		getCurrentPage: function () {
			return this._currentPageNumber;
		},


		setTitle: function pdfViewSetTitle(title) {
			//document.title = title;
		},

		close: function pdfViewClose() {
			if (!this.pdfDocument) {
				return;
			}

			this.pdfDocument.destroy();
			this.pdfDocument = null;

			var thumbsView = this.thumbnailContainer;;
			while (thumbsView.hasChildNodes()) {
				thumbsView.removeChild(thumbsView.lastChild);
			}

			var viewerElement = this.viewerElement;
			while (viewerElement.hasChildNodes()) {
				viewerElement.removeChild(viewerElement.lastChild);
			}
		},

		open: function (url, hotspots, scale, password, pdfDataRangeTransport, args) {
			console.log("Open:" + url);
			this.hotspots = hotspots;
			this.currentScale = ms123.pdf.PDFView.UNKNOWN_SCALE;
			this.currentPoint={x:0,y:0};
			if (this.loading) return;
			if (scale == null) scale = 'page-fit';

			if (this.pdfDocument) {
				this.close();
				//Preferences.reload();
			}

			var parameters = {
				password: password
			};
			if (typeof url === 'string') { // URL
				parameters.url = url;
			} else if (url && 'byteLength' in url) { // ArrayBuffer
				parameters.data = url;
			}
			if (args) {
				for (var prop in args) {
					parameters[prop] = args[prop];
				}
			}

			var self = this;
			self.loading = true;

			var passwordNeeded = function passwordNeeded(updatePassword, reason) {
				//PasswordPrompt.updatePassword = updatePassword;
				//PasswordPrompt.reason = reason;
				//PasswordPrompt.show();
			};

			function getDocumentProgress(progressData) {
				self.progress(progressData.loaded / progressData.total);
			}

			PDFJS.getDocument(parameters, pdfDataRangeTransport, passwordNeeded, getDocumentProgress).then(

			function getDocumentCallback(pdfDocument) {
				self.load(pdfDocument, scale);
				self.loading = false;
			}, function getDocumentError(message, exception) {
				var loadingErrorMessage = self.tr('pdfview.loading_error'); // 'An error occurred while loading the PDF.'
				if (exception && exception.name === 'InvalidPDFException') {
					var loadingErrorMessage = self.tr('pdfview.invalid_file_error'); // 'Invalid or corrupted PDF file.'
				}

				if (exception && exception.name === 'MissingPDFException') {
					var loadingErrorMessage = self.tr('pdfview.missing_file_error'); // 'Missing PDF file.'
				}

				var moreInfo = {
					message: message
				};
				self.error(loadingErrorMessage, moreInfo);
				self.loading = false;
			});
		},
		selectHotspot: function (href) {
			if (this.currentPageView) {
				this.currentPageView.selectHotspot(href);
			}
		},

		fallback: function pdfViewFallback(featureId) {
			return;
		},

		navigateTo: function pdfViewNavigateTo(dest) {
			var destString = '';
			var self = this;

			var goToDestination = function (destRef) {
				self.pendingRefStr = null;
				// dest array looks like that: <page-ref> </XYZ|FitXXX> <args..>
				var pageNumber = destRef instanceof Object ? self.pagesRefMap[destRef.num + ' ' + destRef.gen + ' R'] : (destRef + 1);
				if (pageNumber) {
					if (pageNumber > self.pages.length) {
						pageNumber = self.pages.length;
					}
					var currentPage = self.pages[pageNumber - 1];
					currentPage.scrollIntoView(dest);
				} else {
					self.pdfDocument.getPageIndex(destRef).then(function (pageIndex) {
						var pageNum = pageIndex + 1;
						self.pagesRefMap[destRef.num + ' ' + destRef.gen + ' R'] = pageNum;
						goToDestination(destRef);
					});
				}
			};

			this.destinationsPromise.then(function () {
				if (typeof dest === 'string') {
					destString = dest;
					dest = self.destinations[dest];
				}
				if (!(dest instanceof Array)) {
					return; // invalid destination
				}
				goToDestination(dest[0]);
			});
		},

		getDestinationHash: function pdfViewGetDestinationHash(dest) {
			if (typeof dest === 'string') {
				return this.getAnchorUrl('#' + escape(dest));
			}
			if (dest instanceof Array) {
				var destRef = dest[0]; // see navigateTo method for dest format
				var pageNumber = destRef instanceof Object ? this.pagesRefMap[destRef.num + ' ' + destRef.gen + ' R'] : (destRef + 1);
				if (pageNumber) {
					var pdfOpenParams = this.getAnchorUrl('#page=' + pageNumber);
					var destKind = dest[1];
					if (typeof destKind === 'object' && 'name' in destKind && destKind.name == 'XYZ') {
						var scale = (dest[6] || this.currentScaleValue);
						var scaleNumber = parseFloat(scale);
						if (scaleNumber) {
							scale = scaleNumber * 100;
						}
						pdfOpenParams += '&zoom=' + scale;
						if (dest[2] || dest[3]) {
							pdfOpenParams += ',' + (dest[2] || 0) + ',' + (dest[3] || 0);
						}
					}
					return pdfOpenParams;
				}
			}
			return '';
		},

		getAnchorUrl: function getAnchorUrl(anchor) {
			return anchor;
		},

		error: function pdfViewError(message, moreInfo) {
			console.error("Error:" + message);
		},

		progress: function pdfViewProgress(level) {
			var percent = Math.round(level * 100);
		},

		load: function pdfViewLoad(pdfDocument, scale) {
			var self = this;
			var isOnePageRenderedResolved = false;
			var resolveOnePageRendered = null;
			var onePageRendered = new Promise(function (resolve) {
				resolveOnePageRendered = resolve;
			});

			function bindOnAfterDraw(pageView, thumbnailView) {
				pageView.onAfterDraw = function () {
					if (!isOnePageRenderedResolved) {
						isOnePageRenderedResolved = true;
						resolveOnePageRendered();
					}
					//thumbnailView.setImage(pageView.canvas);
				};
			}

			this.pdfDocument = pdfDocument;

			var pagesCount = pdfDocument.numPages;

			var id = pdfDocument.fingerprint;
			this.documentFingerprint = id;
			this.pageRotation = 0;

			var pages = this.pages = [];
			var pagesRefMap = this.pagesRefMap = {};
			var thumbnails = this.thumbnails = [];

			var resolvePagesPromise;
			var pagesPromise = new Promise(function (resolve) {
				resolvePagesPromise = resolve;
			});
			this.pagesPromise = pagesPromise;

			var firstPagePromise = pdfDocument.getPage(1);
			var viewerElement = this.viewerElement;
			var thumbsElement = this.thumbnailContainer;


			var scaleValue = parseFloat(scale) || 1.0;
			// Fetch a single page so we can get a viewport that will be the default viewport for all pages
			firstPagePromise.then(function (pdfPage) {
				var viewport = pdfPage.getViewport((scaleValue || 1.0) * ms123.pdf.PDFView.CSS_UNITS);
				console.log("viewport:" + viewport);
				for (var pageNum = 1; pageNum <= pagesCount; ++pageNum) {
					var viewportClone = viewport.clone();
					console.log("viewportClone:" + viewportClone);
					var pageView = new ms123.pdf.PageView(self, viewerElement, pageNum, scaleValue, self.navigateTo.bind(self), viewportClone);
					var thumbnailView = new ms123.pdf.ThumbnailView(self, thumbsElement, pageNum, viewportClone);
					bindOnAfterDraw(pageView, thumbnailView);
					pages.push(pageView);
					thumbnails.push(thumbnailView);
					self.currentPageView = pageView;
				}

				// Fetch all the pages since the viewport is needed before printing
				// starts to create the correct size canvas. Wait until one page is
				// rendered so we don't tie up too many resources early on.
				onePageRendered.then(function () {
					if (!PDFJS.disableAutoFetch) {
						var getPagesLeft = pagesCount;
						for (var pageNum = 1; pageNum <= pagesCount; ++pageNum) {
							pdfDocument.getPage(pageNum).then(function (pageNum, pdfPage) {
								var pageView = pages[pageNum - 1];
								if (!pageView.pdfPage) {
									pageView.setPdfPage(pdfPage);
								}
								var refStr = pdfPage.ref.num + ' ' + pdfPage.ref.gen + ' R';
								pagesRefMap[refStr] = pageNum;
								getPagesLeft--;
								if (!getPagesLeft) {
									resolvePagesPromise();
								}
							}.bind(null, pageNum));
						}
					} else {
						// XXX: Printing is semi-broken with auto fetch disabled.
						resolvePagesPromise();
					}
				});

				var event = document.createEvent('CustomEvent');
				event.initCustomEvent('documentload', true, true, {});
				window.dispatchEvent(event);

			});
			Promise.all([firstPagePromise]).then(function resolved() {
				var storedHash = null;
				self.setInitialView(storedHash, scale);
				self.contentElement.focus();
			}, function rejected(errorMsg) {
				console.error(errorMsg);

				firstPagePromise.then(function () {
					self.setInitialView(null, scale);
				});
			});

			pagesPromise.then(function () {});

			var destinationsPromise = this.destinationsPromise = pdfDocument.getDestinations();
			destinationsPromise.then(function (destinations) {
				self.destinations = destinations;
			});

			// outline depends on destinations and pagesRefMap
			var promises = [pagesPromise, destinationsPromise /*, PDFView.animationStartedPromise*/ ];
			Promise.all(promises).then(function () {
				pdfDocument.getOutline().then(function (outline) {});
			});

			pdfDocument.getMetadata().then(function (data) {
				var info = data.info,
					metadata = data.metadata;
				self.documentInfo = info;
				self.metadata = metadata;

				// Provides some basic debug information
				console.log('PDF ' + pdfDocument.fingerprint + ' [' + info.PDFFormatVersion + ' ' + (info.Producer || '-') + ' / ' + (info.Creator || '-') + ']' + (PDFJS.version ? ' (PDF.js: ' + PDFJS.version + ')' : ''));

				var pdfTitle;
				if (metadata && metadata.has('dc:title')) {
					pdfTitle = metadata.get('dc:title');
				}

				if (!pdfTitle && info && info['Title']) {
					pdfTitle = info['Title'];
				}

				if (pdfTitle) {
					self.setTitle(pdfTitle + ' - ' + document.title);
				}

				if (info.IsAcroFormPresent) {
					console.warn('Warning: AcroForm/XFA is not supported');
					this.fallback(PDFJS.UNSUPPORTED_FEATURES.forms);
				}

			});
		},

		setInitialView: function (storedHash, scale) {
			console.log("setInitialView:" + scale);
			this.currentScaleValue = null;
			this._currentPageNumber = 1;
			this.currentPosition = null;

			if (scale) {
				this.setScale(scale);
				this.page = 1;
			} else {
				this.setScale(ms123.pdf.PDFView.DEFAULT_SCALE);
			}

			this.renderHighestPriority();
		},

		renderHighestPriority: function () {
			if (this.idleTimeout) {
				clearTimeout(this.idleTimeout);
				this.idleTimeout = null;
			}

			// Pages have a higher priority than thumbnails, so check them first.
			var visiblePages = this.getVisiblePages();
			var pageView = this.getHighestPriority(visiblePages, this.pages, this.pageViewScroll.down);
			if (pageView) {
				this.renderView(pageView, 'page');
				return;
			}
			var visibleThumbs = this.getVisibleThumbs();
			var thumbView = this.getHighestPriority(visibleThumbs, this.thumbnails, this.thumbnailViewScroll.down);
			if (thumbView) {
				this.renderView(thumbView, 'thumbnail');
				return;
			}
			var self = this;
			this.idleTimeout = setTimeout(function () {
				self.cleanup();
			}, ms123.pdf.PDFView.CLEANUP_TIMEOUT);
		},

		cleanup: function pdfViewCleanup() {
			for (var i = 0, ii = this.pages.length; i < ii; i++) {
				if (this.pages[i] && this.pages[i].renderingState !== ms123.pdf.PDFView.RenderingStates.FINISHED) {
					this.pages[i].reset();
				}
			}
			this.pdfDocument.cleanup();
		},

		getHighestPriority: function (visible, views, scrolledDown) {
			// The state has changed figure out which page has the highest priority to
			// render next (if any).
			// Priority:
			// 1 visible pages
			// 2 if last scrolled down page after the visible pages
			// 2 if last scrolled up page before the visible pages
			var visibleViews = visible.views;

			var numVisible = visibleViews.length;
			if (numVisible === 0) {
				return false;
			}
			for (var i = 0; i < numVisible; ++i) {
				var view = visibleViews[i].view;
				if (!this.isViewFinished(view)) {
					return view;
				}
			}

			// All the visible views have rendered, try to render next/previous pages.
			if (scrolledDown) {
				var nextPageIndex = visible.last.id;
				// ID's start at 1 so no need to add 1.
				if (views[nextPageIndex] && !this.isViewFinished(views[nextPageIndex])) {
					return views[nextPageIndex];
				}
			} else {
				var previousPageIndex = visible.first.id - 2;
				if (views[previousPageIndex] && !this.isViewFinished(views[previousPageIndex])) {
					return views[previousPageIndex];
				}
			}
			return false;
		},

		isViewFinished: function (view) {
			return view.renderingState === ms123.pdf.PDFView.RenderingStates.FINISHED;
		},

		renderView: function pdfViewRender(view, type) {
			var state = view.renderingState;
			switch (state) {
			case ms123.pdf.PDFView.RenderingStates.FINISHED:
				return false;
			case ms123.pdf.PDFView.RenderingStates.PAUSED:
				this.highestPriorityPage = type + view.id;
				view.resume();
				break;
			case ms123.pdf.PDFView.RenderingStates.RUNNING:
				this.highestPriorityPage = type + view.id;
				break;
			case ms123.pdf.PDFView.RenderingStates.INITIAL:
				this.highestPriorityPage = type + view.id;
				view.draw(this.renderHighestPriority.bind(this));
				break;
			}
			return true;
		},

		getVisiblePages: function () {
			if (true /*!PresentationMode.active*/ ) {
				return this.getVisibleElements(this.contentElement, this.pages, true);
			} else {
				// The algorithm in getVisibleElements doesn't work in all browsers and
				// configurations when presentation mode is active.
				var visible = [];
				var currentPage = this.pages[this.page - 1];
				visible.push({
					id: currentPage.id,
					view: currentPage
				});
				return {
					first: currentPage,
					last: currentPage,
					views: visible
				};
			}
		},

		getVisibleThumbs: function pdfViewGetVisibleThumbs() {
			return this.getVisibleElements(this.thumbnailContainer, this.thumbnails);
		},

		getVisibleElements: function (scrollEl, views, sortByVisibility) {
			var top = scrollEl.scrollTop,
				bottom = top + scrollEl.clientHeight;
			var left = scrollEl.scrollLeft,
				right = left + scrollEl.clientWidth;

			var visible = [],
				view;
			var currentHeight, viewHeight, hiddenHeight, percentHeight;
			var currentWidth, viewWidth;
			for (var i = 0, ii = views.length; i < ii; ++i) {
				view = views[i];
				currentHeight = view.el.offsetTop + view.el.clientTop;
				viewHeight = view.el.clientHeight;
				if ((currentHeight + viewHeight) < top) {
					continue;
				}
				if (currentHeight > bottom && scrollEl !== this.thumbnailContainer) {
					break;
				}
				currentWidth = view.el.offsetLeft + view.el.clientLeft;
				viewWidth = view.el.clientWidth;
				if ((currentWidth + viewWidth) < left || currentWidth > right) {
					continue;
				}
				hiddenHeight = Math.max(0, top - currentHeight) + Math.max(0, currentHeight + viewHeight - bottom);
				percentHeight = ((viewHeight - hiddenHeight) * 100 / viewHeight) | 0;
				visible.push({
					id: view.id,
					x: currentWidth,
					y: currentHeight,
					view: view,
					percent: percentHeight
				});
			}

			var first = visible[0];
			var last = visible[visible.length - 1];

			if (sortByVisibility) {
				visible.sort(function (a, b) {
					var pc = a.percent - b.percent;
					if (Math.abs(pc) > 0.001) {
						return -pc;
					}
					return a.id - b.id; // ensure stability
				});
			}
			return {
				first: first,
				last: last,
				views: visible
			};
		},

		beforePrint: function pdfViewSetupBeforePrint() {
			if (!this.supportsPrinting) {
				var printMessage = this.tr('printing_not_supported'); //'Warning: Printing is not fully supported by this browser.'
				this.error(printMessage);
				return;
			}

			var alertNotReady = false;
			if (!this.pages.length) {
				alertNotReady = true;
			} else {
				for (var i = 0, ii = this.pages.length; i < ii; ++i) {
					if (!this.pages[i].pdfPage) {
						alertNotReady = true;
						break;
					}
				}
			}
			if (alertNotReady) {
				var notReadyMessage = this.tr('printing_not_ready'); // 'Warning: The PDF is not fully loaded for printing.'
				window.alert(notReadyMessage);
				return;
			}

			var body = document.querySelector('body');
			body.setAttribute('data-mozPrintCallback', true);
			for (var i = 0, ii = this.pages.length; i < ii; ++i) {
				this.pages[i].beforePrint();
			}
		},

		afterPrint: function pdfViewSetupAfterPrint() {
			var div = document.getElementById('printContainer');
			while (div.hasChildNodes()) {
				div.removeChild(div.lastChild);
			}
		},

		rotatePages: function pdfViewRotatePages(delta) {
			var currentPage = this.pages[this.page - 1];
			this.pageRotation = (this.pageRotation + 360 + delta) % 360;

			for (var i = 0, l = this.pages.length; i < l; i++) {
				var page = this.pages[i];
				page.update(page.scale, this.pageRotation);
			}

			for (var i = 0, l = this.thumbnails.length; i < l; i++) {
				var thumb = this.thumbnails[i];
				thumb.update(this.pageRotation);
			}

			this.setScale(this.currentScaleValue, true);

			this.renderHighestPriority();

			if (currentPage) {
				currentPage.scrollIntoView();
			}
		},

		mouseScroll: function pdfViewMouseScroll(mouseScrollDelta) {
			var currentTime = (new Date()).getTime();
			var storedTime = this.mouseScrollTimeStamp;

			// In case one page has already been flipped there is a cooldown time
			// which has to expire before next page can be scrolled on to.
			if (currentTime > storedTime && currentTime - storedTime < ms123.pdf.PDFView.MOUSE_SCROLL_COOLDOWN_TIME) {
				return;
			}

			// In case the user decides to scroll to the opposite direction than before clear the accumulated delta.
			if ((this.mouseScrollDelta > 0 && mouseScrollDelta < 0) || (this.mouseScrollDelta < 0 && mouseScrollDelta > 0)) {
				this.clearMouseScrollState();
			}

			this.mouseScrollDelta += mouseScrollDelta;

			if (Math.abs(this.mouseScrollDelta) >= ms123.pdf.PDFView.PAGE_FLIP_THRESHOLD) {

				var PageFlipDirection = {
					UP: -1,
					DOWN: 1
				};

				// In presentation mode scroll one page at a time.
				var pageFlipDirection = (this.mouseScrollDelta > 0) ? PageFlipDirection.UP : PageFlipDirection.DOWN;
				this.clearMouseScrollState();
				var currentPage = this.page;

				// In case we are already on the first or the last page there is no need
				// to do anything.
				if ((currentPage == 1 && pageFlipDirection == PageFlipDirection.UP) || (currentPage == this.pages.length && pageFlipDirection == PageFlipDirection.DOWN)) {
					return;
				}

				this.page += pageFlipDirection;
				this.mouseScrollTimeStamp = currentTime;
			}
		},

		/**
		 * This function clears the member attributes used with mouse scrolling in
		 * presentation mode.
		 *
		 * @this {PDFView}
		 */
		clearMouseScrollState: function pdfViewClearMouseScrollState() {
			this.mouseScrollTimeStamp = 0;
			this.mouseScrollDelta = 0;
		},

		updateViewarea: function () {
			if (!this.initialized) {
				return;
			}
			var visible = this.getVisiblePages();
			var visiblePages = visible.views;
			if (visiblePages.length === 0) {
				return;
			}

			this.renderHighestPriority();

			var currentId = this.getCurrentPage();
			var firstPage = visible.first;

			for (var i = 0, ii = visiblePages.length, stillFullyVisible = false;
			i < ii; ++i) {
				var page = visiblePages[i];

				if (page.percent < 100) {
					break;
				}
				if (page.id === this.getCurrentPage()) {
					stillFullyVisible = true;
					break;
				}
			}

			if (!stillFullyVisible) {
				currentId = visiblePages[0].id;
			}

			//if (!PresentationMode.active) {
			this.updateViewareaInProgress = true; // used in "set page"
			this.setCurrentPage(currentId);
			this.updateViewareaInProgress = false;
			//}
			var currentScale = this.currentScale;
			var currentScaleValue = this.currentScaleValue;
			var normalizedScaleValue = parseFloat(currentScaleValue) === currentScale ? Math.round(currentScale * 10000) / 100 : currentScaleValue;

			var pageNumber = firstPage.id;
			var currentPage = this.pages[pageNumber - 1];
			var contentElement = this.contentElement;
			var topLeft = currentPage.getPagePoint((contentElement.scrollLeft - firstPage.x), (contentElement.scrollTop - firstPage.y));
			var intLeft = Math.round(topLeft[0]);
			var intTop = Math.round(topLeft[1]);

			if (false /*PresentationMode.active || PresentationMode.switchInProgress*/ ) {
				this.currentPosition = null;
			} else {
				this.currentPosition = {
					page: pageNumber,
					left: intLeft,
					top: intTop
				};
			}
			var bounds = this.getBounds();
			var data = {
				page: pageNumber,
				left: contentElement.scrollLeft - firstPage.x,
				top: contentElement.scrollTop - firstPage.y,
				scale: currentScale,
				pageWidth: firstPage.view.viewport.width,
				bounds: this.getBounds()
			}
			this.fireDataEvent("change", data, null);
		}
	}
});
