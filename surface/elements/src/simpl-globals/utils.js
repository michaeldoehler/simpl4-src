'use strict';

if (!String.format) {
    String.format = function (format) {
        var args = Array.prototype.slice.call(arguments, 1);
        return format.replace(/{(\d+)}/g, function (match, number) {
            return typeof args[number] !== 'undefined' ? args[number] : match;
        });
    };
}

if (!String.slugify) {
    String.slugify = function (input) {
        return input
            .replace(/^\s\s*/, '') 
            .replace(/\s\s*$/, '') 
            .toLowerCase() 
            .replace(/[^a-z0-9_\-~!\+\s]+/g, '') 
            .replace(/[\s]+/g, '-'); 
    };
}

var Simpl4 = Simpl4 || {};

Simpl4.Utils = (function () {
    var isArrayMatch = function (target, toMatch) {
        var found, targetMap, i, j, cur;

        found = false;
        targetMap = {};

        if (!target || !toMatch) {
            return false;
        }
       
        for (i = 0, j = target.length; i < j; i++) {
            cur = target[i];
            targetMap[cur] = true;
        }

        for (i = 0, j = toMatch.length; !found && (i < j) ; i++) {
            cur = toMatch[i];
            found = !!targetMap[cur];
        }
        return found;
    }

    return {
        isArrayMatch: isArrayMatch,
    }
})();

Simpl4.Cache = (function () {
    var cache = new Cache(-1, false, new Cache.LocalStorageCacheStorage());

    var getItem = function(item) {
        return cache.getItem(item);
    }

    var removeItem = function (item) {
        return cache.removeItem(item);
    }

    var setItem = function (key, value, options) {
        return cache.setItem(key, value, options);
    }

    var removeWhere = function (test) {
        return cache.removeWhere(test);
    }

    return {
        setItem: setItem,
        getItem: getItem,
        removeItem: removeItem,
        removeWhere: removeWhere
    }
})();

