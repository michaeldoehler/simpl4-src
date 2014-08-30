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
	@asset(qx/icon/${qx.icontheme}/16/actions/dialog-ok.png)
	@asset(qxe/demo/*)
*/

/**
 * This is the main application class of your custom application "qxe"
 */
qx.Class.define("ms123.wizard.ExampleWizard", {
	extend: qx.core.Object,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this.base(arguments);

		var wizard = new ms123.wizard.Wizard();
console.log("ExampleWizard:"+wizard);

		wizard.add(this.createPage1("Step 1"));
		wizard.add(this.createPage2("Step 2"));
		wizard.add(this.createPage3("Step 3"));
		wizard.add(this.createPage4("Step 4"));
		wizard.add(this.createPage5("Step 5"));

			context.window.setLayout(new qx.ui.layout.Grow());
		context.window.add( wizard);
		context.window.open();
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: {
		createPage1: function () {
			var page = new ms123.wizard.Page("Step 1");
			page.setLayout(new qx.ui.layout.Canvas());

			var composite = new qx.ui.container.Composite(new qx.ui.layout.HBox(4));

			var label = new qx.ui.basic.Label("Input label");
			label.setAlignY("middle");

			var textField = new qx.ui.form.TextField("Input textfield");

			composite.add(label);
			composite.add(textField);

			page.add(composite, {
				left: 10,
				top: 10
			});

			return page;
		},

		createPage2: function () {
			var page = new ms123.wizard.Page("Step 2");
			page.setLayout(new qx.ui.layout.Canvas());

			var composite = new qx.ui.container.Composite(new qx.ui.layout.HBox(4));

			var label = new qx.ui.basic.Label("Input label");
			label.setAlignY("middle");

			var textField = new qx.ui.form.TextField("Input textfield");

			composite.add(label);
			composite.add(textField);

			page.add(composite, {
				left: 10,
				top: 10
			});

			return page;
		},

		createPage3: function () {
			var page = new ms123.wizard.Page("Step 3");
			page.setLayout(new qx.ui.layout.Canvas());

			var composite = new qx.ui.container.Composite(new qx.ui.layout.HBox(4));

			var label = new qx.ui.basic.Label("Input label");
			label.setAlignY("middle");

			var textField = new qx.ui.form.TextField("Input textfield");

			composite.add(label);
			composite.add(textField);

			page.add(composite, {
				left: 10,
				top: 10
			});

			return page;
		},

		createPage4: function () {
			var page = new ms123.wizard.Page("Step 4");
			page.setLayout(new qx.ui.layout.Canvas());

			var composite = new qx.ui.container.Composite(new qx.ui.layout.HBox(4));

			var label = new qx.ui.basic.Label("Input label");
			label.setAlignY("middle");

			var textField = new qx.ui.form.TextField("Input textfield");

			composite.add(label);
			composite.add(textField);

			page.add(composite, {
				left: 10,
				top: 10
			});

			return page;
		},

		createPage5: function () {
			var page = new ms123.wizard.Page("Step 5");
			page.setLayout(new qx.ui.layout.Canvas());

			var composite = new qx.ui.container.Composite(new qx.ui.layout.HBox(4));

			var label = new qx.ui.basic.Label("Input label");
			label.setAlignY("middle");

			var textField = new qx.ui.form.TextField("Input textfield");

			composite.add(label);
			composite.add(textField);

			page.add(composite, {
				left: 10,
				top: 10
			});

			return page;
		}
	}
});
