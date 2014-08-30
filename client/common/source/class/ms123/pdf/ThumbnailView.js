/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @ignore(PDFJS.*)
 * @ignore(Promise*)
 * @ignore(jQuery*)
 * @lint ignoreDeprecated(alert,eval)
 * @ignore(alert*)
 */
qx.Class.define("ms123.pdf.ThumbnailView", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (pdfView, container, id, defaultViewport) {
		this.base(arguments);
		this.pdfView = pdfView;
		var anchor = document.createElement('a');
		anchor.href = pdfView.getAnchorUrl('#page=' + id);
		anchor.title = this.tr('pdfview.thumb_page_title' + ":" + id);
		anchor.onclick = function stopNavigation() {
			pdfView.setCurrentPage(id);
			return false;
		};
		var naviElements = {};
		this.pdfPage = undefined;
		this.viewport = defaultViewport;
		this.pdfPageRotate = defaultViewport.rotation;

		this.rotation = 0;
		this.pageWidth = this.viewport.width;
		this.pageHeight = this.viewport.height;
		this.pageRatio = this.pageWidth / this.pageHeight;
		this.id = id;

		this.canvasWidth = 150;
		this.canvasHeight = this.canvasWidth / this.pageWidth * this.pageHeight;
		this.scale = (this.canvasWidth / this.pageWidth);

		var thumbDiv = this.el = document.createElement('div');
		thumbDiv.id = 'thumbnailContainer' + id;
		thumbDiv.className = 'thumbnail';
		thumbDiv.style.border = "2px solid #555555";
		thumbDiv.style.position = 'absolute';
		thumbDiv.style.zIndex = 100;
		thumbDiv.style.left = '1px';
		thumbDiv.style.bottom = '1px';
		this.thumbDiv = thumbDiv;

		if (id === 1) {
			thumbDiv.classList.add('selected');
		}


		var ratio = this.pageWidth / this.canvasWidth;
		var nid = ms123.util.IdGen.id("navi");

		var left = 0;
		var top = 0;
		var bounds = pdfView.getBounds();
		var navi = document.createElement('div');
		navi.className = 'thumbnailNavi';
		navi.id = nid;
		navi.style.position = 'absolute';
		navi.style.border = '2px solid #900000';
		navi.style.width = (bounds.width / ratio) + 'px';
		navi.style.height = (bounds.height / ratio) + 'px';
		navi.style.backgroundColor = 'transparent';

		naviElements[id] = navi;
		thumbDiv.appendChild(navi);

		var ring = document.createElement('div');
		ring.className = 'thumbnailSelectionRing';
		ring.style.width = this.canvasWidth + 'px';
		ring.style.height = this.canvasHeight + 'px';
		this.ring = ring;

		thumbDiv.appendChild(ring);
		anchor.appendChild(thumbDiv);
		container.appendChild(anchor);

		var self = this;
		jQuery("#" + nid).draggable({
			drag: function (event, ui) {
				var xDiff = (event.clientX * ratio) - self.clientXStart;
				var yDiff = (event.clientY * ratio) - self.clientYStart;
				pdfView.contentElement.scrollTop = self.scrollTopStart + yDiff;
				pdfView.contentElement.scrollLeft = self.scrollLeftStart + xDiff;
			},
			start: function (event, ui) {
				console.log("start");
			},
			stop: function (event, ui) {
				console.log("stop");
			}
		});
		jQuery("#" + nid).on("dragstart", function (event, ui) {
			console.log("dragstart");
			self.scrollLeftStart = pdfView.contentElement.scrollLeft;
			self.scrollTopStart = pdfView.contentElement.scrollTop;
			self.clientXStart = event.clientX * ratio;
			self.clientYStart = event.clientY * ratio;
		});

		this.addListener('appear', function () {}, this);


		this.hasImage = false;
		this.renderingState = ms123.pdf.PDFView.RenderingStates.INITIAL;
		pdfView.addListener('change', function (e) {
			var data = e.getData();
			var navi = naviElements[data.page];
			ratio = data.pageWidth / this.canvasWidth;
			var bounds = data.bounds;

			var left = data.left / ratio;
			var top = data.top / ratio;
			var width = bounds.width / ratio;
			var height = bounds.height / ratio;

			if (width > this.canvasWidth) {
				width = this.canvasWidth
				left = 0;
			}
			if (height > this.canvasHeight) {
				height = this.canvasHeight
				top = 0;
			}
			navi.style.left = (left) + 'px';
			navi.style.top = (top) + 'px';
			navi.style.width = (width) + 'px';
			navi.style.height = (height) + 'px';
		}, this);
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
		setPdfPage: function thumbnailViewSetPdfPage(pdfPage) {
			this.pdfPage = pdfPage;
			this.pdfPageRotate = pdfPage.rotate;
			var totalRotation = (this.rotation + this.pdfPageRotate) % 360;
			this.viewport = pdfPage.getViewport(1, totalRotation);
			this.update();
		},

		update: function thumbnailViewUpdate(rotation) {
			if (rotation !== undefined) {
				this.rotation = rotation;
			}
			var totalRotation = (this.rotation + this.pdfPageRotate) % 360;
			this.viewport = this.viewport.clone({
				scale: 1,
				rotation: totalRotation
			});
			this.pageWidth = this.viewport.width;
			this.pageHeight = this.viewport.height;
			this.pageRatio = this.pageWidth / this.pageHeight;

			this.canvasHeight = this.canvasWidth / this.pageWidth * this.pageHeight;
			this.scale = (this.canvasWidth / this.pageWidth);

			var thumbDiv = this.thumbDiv;
			var ring = this.ring;
			thumbDiv.removeAttribute('data-loaded');
			ring.textContent = '';
			ring.style.width = this.canvasWidth + 'px';
			ring.style.height = this.canvasHeight + 'px';

			this.hasImage = false;
			this.renderingState = ms123.pdf.PDFView.RenderingStates.INITIAL;
			this.resume = null;
		},

		getPageDrawContext: function thumbnailViewGetPageDrawContext() {
			var canvas = document.createElement('canvas');
			canvas.id = 'thumbnail' + this.id;

			canvas.width = this.canvasWidth;
			canvas.height = this.canvasHeight;
			canvas.className = 'thumbnailImage';
			canvas.setAttribute('aria-label', this.tr('pdfview.thumb_page_canvas') + ":" + this.id);

			var thumbDiv = this.thumbDiv;
			var ring = this.ring;
			thumbDiv.setAttribute('data-loaded', true);

			ring.appendChild(canvas);

			var ctx = canvas.getContext('2d');
			ctx.save();
			ctx.fillStyle = 'rgb(255, 255, 255)';
			ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);
			ctx.restore();
			return ctx;
		},

		drawingRequired: function thumbnailViewDrawingRequired() {
			return !this.hasImage;
		},

		draw: function thumbnailViewDraw(callback) {
			if (!this.pdfPage) {
				var promise = this.pdfView.getPage(this.id);
				promise.then(function (pdfPage) {
					this.setPdfPage(pdfPage);
					this.draw(callback);
				}.bind(this));
				return;
			}

			if (this.renderingState !== ms123.pdf.PDFView.RenderingStates.INITIAL) {
				console.error('Must be in new state before drawing');
			}

			this.renderingState = ms123.pdf.PDFView.RenderingStates.RUNNING;
			if (this.hasImage) {
				callback();
				return;
			}

			var self = this;
			var ctx = this.getPageDrawContext();
			var drawViewport = this.viewport.clone({
				scale: this.scale
			});
			var renderContext = {
				canvasContext: ctx,
				viewport: drawViewport,
				continueCallback: function (cont) {
					if (self.pdfView.highestPriorityPage !== 'thumbnail' + self.id) {
						self.renderingState = ms123.pdf.PDFView.RenderingStates.PAUSED;
						self.resume = function () {
							self.renderingState = ms123.pdf.PDFView.RenderingStates.RUNNING;
							cont();
						};
						return;
					}
					cont();
				}
			};
			this.pdfPage.render(renderContext).promise.then(

			function pdfPageRenderCallback() {
				self.renderingState = ms123.pdf.PDFView.RenderingStates.FINISHED;
				callback();
			}, function pdfPageRenderError(error) {
				self.renderingState = ms123.pdf.PDFView.RenderingStates.FINISHED;
				callback();
			});
			this.hasImage = true;
		},

		setImage: function thumbnailViewSetImage(img) {
			if (!this.pdfPage) {
				var promise = this.pdfView.getPage(this.id);
				promise.then(function (pdfPage) {
					this.setPdfPage(pdfPage);
					this.setImage(img);
				}.bind(this));
				return;
			}
			if (this.hasImage || !img) {
				return;
			}
			this.renderingState = ms123.pdf.PDFView.RenderingStates.FINISHED;
			var ctx = this.getPageDrawContext();
			ctx.drawImage(img, 0, 0, img.width, img.height, 0, 0, ctx.canvas.width, ctx.canvas.height);

			this.hasImage = true;
		}

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
