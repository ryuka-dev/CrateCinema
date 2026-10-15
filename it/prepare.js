// Installs the test fixtures into ../run. Run it with the server stopped, after the server has
// started once (so ExcellentCrates has written its default config). Does not touch eula.txt.
const fs = require('fs');
const path = require('path');

const run = path.join(__dirname, '..', process.env.CC_RUN || 'run');
const fixtures = path.join(__dirname, 'fixtures');

function copyTree(from, to) {
  for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
    const src = path.join(from, entry.name);
    const dst = path.join(to, entry.name);
    if (entry.isDirectory()) {
      fs.mkdirSync(dst, { recursive: true });
      copyTree(src, dst);
    } else {
      fs.copyFileSync(src, dst);
      console.log('wrote', path.relative(run, dst));
    }
  }
}

function edit(file, pattern, replacement) {
  const text = fs.readFileSync(file, 'utf8');
  if (!pattern.test(text)) throw new Error(`pattern not found in ${file}: ${pattern}`);
  fs.writeFileSync(file, text.replace(pattern, replacement));
  console.log('edited', path.relative(run, file));
}

fs.copyFileSync(path.join(fixtures, 'server.properties'), path.join(run, 'server.properties'));
console.log('wrote server.properties');
copyTree(path.join(fixtures, 'ExcellentCrates'), path.join(run, 'plugins', 'ExcellentCrates'));
fs.mkdirSync(path.join(run, 'plugins', 'CrateCinema'), { recursive: true });
copyTree(path.join(fixtures, 'CrateCinema'), path.join(run, 'plugins', 'CrateCinema'));

// Our default config, with debug logging on: the tests read our debug lines.
fs.copyFileSync(path.join(__dirname, '..', 'src', 'main', 'resources', 'config.yml'), path.join(run, 'plugins', 'CrateCinema', 'config.yml'));
edit(path.join(run, 'plugins', 'CrateCinema', 'config.yml'), /^debug: (true|false)/m, 'debug: true');
// ExcellentCrates' 3-second interaction cooldown would race the "second opening" test.
edit(path.join(run, 'plugins', 'ExcellentCrates', 'config', 'crates.interaction.yml'),
  /(cooldown:\n(?:\s*#.*\n)*\s*enabled: )(true|false)/, '$1false');
