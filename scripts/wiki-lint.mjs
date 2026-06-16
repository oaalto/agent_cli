#!/usr/bin/env node
import fs from 'fs';
import path from 'path';

const root = process.cwd();
const wikiDir = path.join(root, 'docs', 'wiki');
const pathMapPath = path.join(wikiDir, 'path-map.json');

function fail(msg){ console.error(msg); process.exitCode = 2; }
function ok(msg){ console.log(msg); }

const files = [];
function walk(dir){
  for(const name of fs.readdirSync(dir)){
    const full = path.join(dir, name);
    const stat = fs.statSync(full);
    if(stat.isDirectory()) walk(full);
    else if(name.endsWith('.md')) files.push(full);
  }
}

if(!fs.existsSync(wikiDir)){
  fail(`Wiki directory not found: ${wikiDir}`);
  process.exit(2);
}

walk(wikiDir);

// Exclude schema/index/log from strict frontmatter enforcement
const exclude = new Set(['schema.md','index.md','log.md']);

let errors = 0;
for(const f of files){
  const name = path.basename(f);
  if(exclude.has(name)) continue;
  const content = fs.readFileSync(f, 'utf8');
  const m = content.match(/^---\s*[\r\n]+([\s\S]*?)\r?\n---/);
  if(!m){
    console.error(`Missing frontmatter in ${path.relative(root,f)}`);
    errors++;
    continue;
  }
  const fm = m[1];
  const required = ['title:','type:','status:','updated:','sources:'];
  for(const r of required){
    if(!new RegExp('^' + r, 'm').test(fm)){
      console.error(`Frontmatter missing '${r.replace(':','')}' in ${path.relative(root,f)}`);
      errors++;
    }
  }
}

// Validate path-map.json presence and mappings
if(!fs.existsSync(pathMapPath)){
  console.error(`Missing path-map.json at ${path.relative(root,pathMapPath)}`);
  errors++;
} else {
  try{
    const pm = JSON.parse(fs.readFileSync(pathMapPath,'utf8'));
    if(Array.isArray(pm.mappings)){
      for(const map of pm.mappings){
        if(map.allowSkip === false && Array.isArray(map.wikiPages)){
          for(const wp of map.wikiPages){
            const wpPath = path.join(wikiDir, wp.replace(/\//g,path.sep));
            if(!fs.existsSync(wpPath)){
              console.error(`Path-map requires wiki page but file missing: ${wpPath}`);
              errors++;
            }
          }
        }
      }
    } else {
      console.error('path-map.json missing "mappings" array.');
      errors++;
    }
  } catch(e){
    console.error('Failed to parse path-map.json:', e.message);
    errors++;
  }
}

if(errors){
  fail(`wiki-lint found ${errors} error(s).`);
} else {
  ok('wiki-lint: no mechanical issues found.');
}

if(process.exitCode) process.exit(process.exitCode);
