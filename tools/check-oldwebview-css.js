#!/usr/bin/env node
/* ============================================================================
   MAYA — OLD WEBVIEW CSS CHECK  (v4.1.0 IRONCLAD)
   ----------------------------------------------------------------------------
   Do kaam karta hai:

   A) LINT — "MAYA UI KIT v5" layer mein koi BANNED CSS feature to nahi
      (color-mix, accent-color, inset:, gap:, env(), min/max/clamp, display:grid,
      :is/:where). Contract: docs/SETTINGS-UI-ARCHITECTURE.md

   B) SIMULATE — purane WebView (Chrome < 87) ki tarah un declarations ko phenk
      kar check karta hai ke Settings ke har control par ab bhi visible
      background/border/size bacha hai.

   Chalane ka tareeqa:
     npm i --no-save css-tree
     node tools/check-oldwebview-css.js
   ========================================================================= */
'use strict';

var fs = require('fs');
var path = require('path');
var csstree;
try { csstree = require('css-tree'); }
catch (e) { console.error('css-tree nahi mila. Chalao:  npm i --no-save css-tree'); process.exit(2); }

var file = process.argv[2] || path.join(__dirname, '..', 'public', 'index.html');
var html = fs.readFileSync(file, 'utf8');
var css = html.slice(html.indexOf('<style>') + 7, html.indexOf('</style>'));

var fail = 0;
function ok(m)  { console.log('  \u2705 ' + m); }
function bad(m) { fail++; console.log('  \u274C ' + m); }

/* ------------------------------------------------------------ 0) PARSE */
console.log('\n0. CSS PARSE');
var parseErrors = [];
var ast = csstree.parse(css, { positions: true, onParseError: function (e) { parseErrors.push('line ' + e.line + ': ' + e.message); } });
parseErrors.length ? parseErrors.forEach(function (e) { bad('parse error \u2014 ' + e); })
                   : ok('poora stylesheet bina error parse hua');

/* --------------------------------------------------- A) UI KIT LAYER LINT */
console.log('\nA. UI KIT LAYER LINT (banned features)');
var kitStart = css.indexOf('MAYA UI KIT v5');
/* comment ke bilkul shuru se slice karo warna adhoora comment bach jata hai */
if (kitStart > 0) { var o = css.lastIndexOf('/*', kitStart); if (o >= 0) kitStart = o; }
if (kitStart < 0) { bad('UI KIT v5 layer nahi mila'); }
else {
  /* comments hata do (warna comment ka text selector/pretext ban jata hai) aur
     @supports blocks nikaal do (wo definition ke tor par guarded hain) */
  var kitCss = css.slice(kitStart).replace(/\/\*[\s\S]*?\*\//g, '');
  var kitAst = csstree.parse(kitCss);
  csstree.walk(kitAst, { visit: 'Atrule', enter: function (node, item, list) {
    if (node.name === 'supports') list.remove(item);
  }});
  var BANNED_VALUE = /color-mix\(|env\(|(^|[^-\w])min\(|(^|[^-\w])max\(|clamp\(/;
  var BANNED_PROP  = /^(inset|gap|row-gap|column-gap|accent-color|aspect-ratio|backdrop-filter)$/;
  var hits = [];
  csstree.walk(kitAst, { visit: 'Declaration', enter: function (d) {
    var v = csstree.generate(d.value);
    if (BANNED_PROP.test(d.property)) hits.push(d.property + ': ' + v + '   (property banned)');
    else if (BANNED_VALUE.test(v))    hits.push(d.property + ': ' + v + '   (value banned)');
    else if (d.property === 'display' && /grid/.test(v)) hits.push('display: ' + v + '   (grid banned)');
  }});
  csstree.walk(kitAst, { visit: 'Rule', enter: function (r) {
    var s = csstree.generate(r.prelude).replace(/\/\*[\s\S]*?\*\//g, '');   // comments hata do
    if (/:is\(|:where\(/.test(s)) hits.push('selector ' + s.trim() + '   (:is/:where banned)');
  }});
  hits.length ? hits.forEach(function (h) { bad(h); })
              : ok('UI KIT layer saaf hai \u2014 koi banned CSS nahi');
}

/* ------------------------------------------- B) OLD WEBVIEW SIMULATION */
console.log('\nB. OLD WEBVIEW SIMULATION (Chrome < 87)');
var MODERN = /color-mix|accent-color|env\(|(^|[^-\w])min\(|(^|[^-\w])max\(|clamp\(/;
var dropped = 0;
csstree.walk(ast, { visit: 'Atrule', enter: function (node, item, list) {
  if (node.name === 'supports') list.remove(item);
}});
csstree.walk(ast, { visit: 'Declaration', enter: function (node, item, list) {
  var v = csstree.generate(node.value);
  if (MODERN.test(v) || node.property === 'inset' || /^(gap|row-gap|column-gap)$/.test(node.property)) {
    dropped++; list.remove(item);
  }
}});

var map = {};
csstree.walk(ast, { visit: 'Rule', enter: function (rule) {
  var sel = csstree.generate(rule.prelude);
  rule.block.children.forEach(function (d) {
    if (d.type !== 'Declaration') return;
    sel.split(',').forEach(function (s) {
      s = s.trim(); if (!map[s]) map[s] = {};
      map[s][d.property] = csstree.generate(d.value);
    });
  });
}});

var checks = [
  ['.ui-group',   ['background-color', 'border', 'border-radius']],
  ['.ui-head',    ['display', 'padding']],
  ['.ui-body',    ['display', 'padding']],
  ['.ui-label',   ['background-color', 'border', 'min-height']],
  ['.ui-input',   ['background-color', 'border', 'min-height', 'box-sizing']],
  ['.ui-slider',  ['background-color', 'border', 'height', 'position']],
  ['.ui-track',   ['background-color', 'height', 'top', 'left', 'right']],
  ['.ui-fill',    ['background-color', 'height', 'width']],
  ['.ui-thumb',   ['background-color', 'border', 'width', 'height', 'margin-left']],
  ['.ui-step',    ['background-color', 'border', 'width', 'height']],
  ['.ui-row',     ['display', 'background-color', 'border']],
  ['.ui-row-txt', ['display', 'vertical-align']],
  ['.ui-row-ctl', ['display', 'width', 'text-align']],
  ['.ui-sw',      ['position', 'width', 'height']],
  ['.ui-sw-t',    ['background-color', 'border', 'width', 'height', 'top', 'left']],
  ['.ui-sw-k',    ['background-color', 'width', 'height', 'top', 'left']],
  ['.ui-sw input:checked~.ui-sw-t', ['background-color']],
  ['.ui-sw input:checked~.ui-sw-k', ['left']],
  ['.ui-sw.is-on .ui-sw-t',         ['background-color']],
  ['.ui-btn',     ['background-color', 'min-height', 'width']],
  ['#tab-set.active', ['display']],
  ['.ui-native',  ['position', 'left']]
];
checks.forEach(function (c) {
  var sel = c[0], props = c[1], got = map[sel] || {};
  var missing = props.filter(function (p) { return !(p in got); });
  missing.length
    ? bad(sel + '  \u2192  MISSING: ' + missing.join(', '))
    : ok(sel + '  \u2192  ' + props.map(function (p) { return p + ':' + got[p]; }).join(' | '));
});

console.log('\npurane WebView par drop hone wali declarations: ' + dropped);
console.log('\n' + '='.repeat(58));
console.log(fail === 0
  ? '\u2705 CSS CHECK PASS \u2014 Settings ka har control purane WebView par bhi visible'
  : '\u274C CSS CHECK FAIL \u2014 ' + fail + ' masla');
console.log('='.repeat(58));
process.exit(fail ? 1 : 0);
