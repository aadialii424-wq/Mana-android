#!/usr/bin/env node
/* ============================================================================
   MAYA — OLD WEBVIEW CSS CHECK  (v4.0.2)
   ----------------------------------------------------------------------------
   Kya karta hai: public/index.html ka <style> parse karta hai, phir PURANE
   Android System WebView (Chrome < 87) ka behaviour simulate karta hai —
   yani `color-mix()`, `accent-color`, `inset`, flex `gap`, `env()`, `min()`
   wali declarations aur `@supports` blocks nikaal deta hai (browser inhe
   chup-chaap drop kar deta hai).

   Uske baad check karta hai ke Settings ke saare form controls (inputs,
   labels, range sliders, switches) par ab bhi visible background + border +
   size bacha hai ya nahi. Agar kuch bhi missing ho to non-zero exit.

   Chalane ka tareeqa:
     npm i --no-save css-tree
     node tools/check-oldwebview-css.js
   ========================================================================= */
'use strict';

var fs = require('fs');
var path = require('path');
var csstree;
try {
  csstree = require('css-tree');
} catch (e) {
  console.error('css-tree nahi mila. Pehle chalao:  npm i --no-save css-tree');
  process.exit(2);
}

var file = process.argv[2] || path.join(__dirname, '..', 'public', 'index.html');
var html = fs.readFileSync(file, 'utf8');
var css = html.slice(html.indexOf('<style>') + 7, html.indexOf('</style>'));

/* 1) Parse — koi bhi syntax error = fail (invalid CSS purane parser ko zyada tang karta hai) */
var parseErrors = [];
var ast = csstree.parse(css, {
  positions: true,
  onParseError: function (err) { parseErrors.push('line ' + err.line + ': ' + err.message); }
});
parseErrors.forEach(function (e) { console.log('⚠ CSS PARSE ERROR — ' + e); });

/* 2) Purane WebView ka simulation */
var MODERN = /color-mix|accent-color|env\(|min\(|max\(|clamp\(/;
var dropped = 0;
csstree.walk(ast, { visit: 'Atrule', enter: function (node, item, list) {
  if (node.name === 'supports') { list.remove(item); }
}});
csstree.walk(ast, { visit: 'Declaration', enter: function (node, item, list) {
  var v = csstree.generate(node.value);
  if (MODERN.test(v) || node.property === 'inset' || /^(gap|row-gap|column-gap)$/.test(node.property)) {
    dropped++; list.remove(item);
  }
}});

/* 3) Bache hue declarations selector ke hisaab se jama karo */
var map = {};
csstree.walk(ast, { visit: 'Rule', enter: function (rule) {
  var sel = csstree.generate(rule.prelude);
  rule.block.children.forEach(function (d) {
    if (d.type !== 'Declaration') return;
    sel.split(',').forEach(function (s) {
      s = s.trim();
      if (!map[s]) map[s] = {};
      map[s][d.property] = csstree.generate(d.value);
    });
  });
}});

/* 4) Settings ke zaroori controls */
var checks = [
  ['.f',                                                ['background-color', 'border', 'min-height']],
  ['label.f',                                           ['background-color', 'border', 'padding']],
  ['input[type=range]',                                 ['background-color', 'border', 'height']],
  ['input[type=range]::-webkit-slider-runnable-track',  ['background-color', 'height']],
  ['input[type=range]::-webkit-slider-thumb',           ['background-color', 'width', 'height']],
  ['.switch i',                                         ['background-color', 'width', 'height', 'top', 'left']],
  ['.switch input:checked+i',                           ['background-color']],
  ['.switch i::before',                                 ['background-color', 'width', 'height', 'left']],
  ['.switchrow',                                        ['background-color', 'border', 'min-height']],
  ['.setgrp',                                           ['background-color', 'border']],
  ['main',                                              ['min-height']],   // scroll chain
  ['.tab',                                              ['overflow-y', 'min-height']]
];

var fail = parseErrors.length;
checks.forEach(function (c) {
  var sel = c[0], props = c[1], got = map[sel] || {};
  var missing = props.filter(function (p) { return !(p in got); });
  if (missing.length) {
    fail++;
    console.log('❌ ' + sel + '  →  MISSING: ' + missing.join(', '));
  } else {
    console.log('✅ ' + sel + '  →  ' + props.map(function (p) { return p + ':' + got[p]; }).join(' | '));
  }
});

console.log('\npurane WebView par drop hone wali declarations: ' + dropped);
console.log(fail
  ? '\nRESULT ❌ — ' + fail + ' cheez purane WebView par invisible/toot sakti hai'
  : '\nRESULT ✅ — Settings ke saare controls purane WebView par bhi visible hain');
process.exit(fail ? 1 : 0);
