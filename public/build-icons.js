#!/usr/bin/env node
/**
 * MAYA Icon Generator — 100% free, no dependencies
 * Node.js 18+ canvas alternative: generates PNG icons using pure JS
 * Run: node build-icons.js
 */
const fs = require('fs');
const path = require('path');

const ICONS_DIR = path.join(__dirname, 'icons');
if (!fs.existsSync(ICONS_DIR)) fs.mkdirSync(ICONS_DIR, { recursive: true });

// Generate a minimal valid PNG with MAYA branding
function createPNG(size, bgColor, fgColor) {
  // We'll create a simple 1x1 pixel PNG and scale it
  // For production, use a proper image library
  // For now, we create an SVG-based approach
  
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="${size}" height="${size}" fill="${bgColor}" rx="${size * 0.15}"/>
  <circle cx="${size/2}" cy="${size * 0.42}" r="${size * 0.22}" fill="none" stroke="${fgColor}" stroke-width="${size * 0.015}"/>
  <path d="M${size*0.35},${size*0.62} L${size*0.35},${size*0.42} L${size*0.5},${size*0.56} L${size*0.65},${size*0.42} L${size*0.65},${size*0.62}" 
    fill="none" stroke="${fgColor}" stroke-width="${size * 0.06}" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`;
  
  return svg;
}

const sizes = [96, 192, 512];
sizes.forEach(size => {
  const svg = createPNG(size, '#050B14', '#22D3EE');
  const filePath = path.join(ICONS_DIR, `icon-${size}.svg`);
  fs.writeFileSync(filePath, svg);
  console.log(`✅ Created icon-${size}.svg`);
});

// Also create favicon as SVG
const favicon = createPNG(32, '#050B14', '#22D3EE');
fs.writeFileSync(path.join(__dirname, 'favicon.svg'), favicon);
console.log('✅ Created favicon.svg');
console.log('\n🎉 All icons generated! (SVG format — supported by all modern browsers)');
console.log('📌 For Apple Touch Icon, convert SVGs to PNG using https://convertio.co/svg-png/');
