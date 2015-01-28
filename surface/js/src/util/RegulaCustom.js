 regula.custom({
      name: "AssertTrue",
      defaultMessage: "The answer must be equal to 42",
      validator: function() {
         return this.value === true;
      }
  });
 regula.custom({
      name: "AssertFalse",
      defaultMessage: "The answer must be equal to 42",
      validator: function() {
         return this.value !== true;
      }
  });
