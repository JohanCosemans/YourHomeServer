/**
 * parses and returns URI query parameters 
 * 
 * @param {string} param parm
 * @param {bool?} asArray if true, returns an array instead of a scalar 
 * @returns {Object|Array} 
 */
function getURIParameter(param, asArray) {
    return document.location.search.substring(1).split('&').reduce(function(p,c) {
        var parts = c.split('=', 2).map(function(param) { return decodeURIComponent(param); });
        if(parts.length == 0 || parts[0] != param) return (p instanceof Array) && !asArray ? null : p;
        return asArray ? p.concat(parts.concat(true)[1]) : parts.concat(true)[1];
    }, []);
}