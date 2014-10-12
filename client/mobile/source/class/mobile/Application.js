/* ************************************************************************

   Copyright:

   License:

   Authors:

************************************************************************ */

/**
 * This is the main application class of your custom application "mobile"
 *
 * @asset(mobile/*)
 */
qx.Class.define("mobile.Application",
{
  extend : qx.application.Mobile,


  members :
  {

    /**
     * This method contains the initial application code and gets called
     * during startup of the application
     */
    main : function()
    {
      // Call super class
      this.base(arguments);

      // Enable logging in debug variant
      if (qx.core.Environment.get("qx.debug"))
      {
        // support native logging capabilities, e.g. Firebug for Firefox
        qx.log.appender.Native;
        // support additional cross-browser console.
        // Trigger a "longtap" event on the navigation bar for opening it.
        qx.log.appender.Console;
      }

      /*
      -------------------------------------------------------------------------
        Below is your actual application code...
        Remove or edit the following code to create your application.
      -------------------------------------------------------------------------
      */

      var login = new mobile.page.Login();
      var overview = new mobile.page.Overview();
      var basic = new mobile.page.Basic();
      var testhtml = new mobile.page.Testhtml();

      // Add the pages to the page manager.
      var manager = new qx.ui.mobile.page.Manager();
      manager.addMaster(overview);
      manager.addDetail([
        basic,
        login,
				testhtml
      ]);

      if (qx.core.Environment.get("device.type") == "tablet" ||
       qx.core.Environment.get("device.type") == "desktop") {
        this.getRouting().onGet("/.*", this._show, overview);
        this.getRouting().onGet("/", this._show, basic);
      }
      // Initialize the application routing
      this.getRouting().onGet("/", this._show, overview);
      this.getRouting().onGet("/basic", this._show, basic);
      this.getRouting().onGet("/login", this._show, login);
      this.getRouting().onGet("/testhtml", this._show, testhtml);

      this.getRouting().init();
    },


    /**
     * Default behaviour when a route matches. Displays the corresponding page on screen.
     * @param data {Map} the animation properties
     */
    _show : function(data) {
      this.show(data.customData);
    }
  }
});
