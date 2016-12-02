var gulp = require('gulp');
var runSequence = require('run-sequence');

// Include plugins
var plugins = require('gulp-load-plugins')({
  pattern: ['gulp-*', 'gulp.*'],
  replaceString: /\bgulp[\-.]/
});

const del = require('del');
const tscConfig = require('./tsconfig.json');

var tsProject = plugins.typescript.createProject('tsconfig.json');

// define default destination folders
var dest = 'build/';
var bundleDest = 'dist/';

// download web-fonts locally
gulp.task('fonts', function () {
	return gulp.src('./fonts.list')
		.pipe(plugins.googleWebfonts({}))
		.pipe(gulp.dest(dest + 'fonts'))
});

// copy dependencies
gulp.task('copy:libs', function() {
  return gulp.src(plugins.npmFiles(), { base: './' })
    .pipe(gulp.dest(dest))
});

gulp.task('bundle:css', function() {
  return gulp
    .src([
      dest + 'jspm_packages/npm/**/cal-heatmap.css',
      dest + 'jspm_packages/npm/**/c3.css',
      dest + 'jspm_packages/npm/**/material.css',
      dest + 'css/**/*',
    ], { base: dest })
    .pipe(plugins.concat('iot-connector.css'))
    .pipe(gulp.dest(bundleDest));
})

gulp.task('bundle:static', function() {
  return gulp
    .src([
      dest + 'images/**/*',
      dest + 'fonts/**/*',
      dest + 'app/**/*.html',
    ], { base: dest })
    .pipe(gulp.dest(bundleDest))
})

gulp.task('bundle:config', function() {
  return gulp
    .src([
      dest + 'iot-connector.config.js',
    ], { base: dest })
    .pipe(gulp.dest(bundleDest))
})

gulp.task('bundle:index', function() {
  return gulp.src('index-bundle.html')
    .pipe(plugins.rename(function(path) {
      path.basename = path.basename.slice(0, -7);
    }))
    .pipe(gulp.dest(bundleDest));
});

// dependencies that for some reason cannot be loaded by SystemJS
gulp.task('bundle:dependencies', function() {
  return gulp
    .src([
      dest + 'jspm_packages/npm/d3*/d3.js',
      dest + 'jspm_packages/npm/c3*/c3.js',
      dest + 'jspm_packages/npm/material-design-lite*/dist/material.min.js',
      dest + 'jspm_packages/npm/cal-heatmap*/cal-heatmap.js',
      dest + 'node_modules/viz.js/viz.js',
    ])
    .pipe(plugins.concat('dependencies.js'))
    .pipe(gulp.dest(bundleDest))
});

gulp.task('bundle:webpack', function () {
  return gulp.src('build/app/main.js')
    .pipe(plugins.webpack())
    .pipe(gulp.dest(bundleDest));
});

gulp.task('bundle', function() {
  runSequence('build', ['bundle:webpack', 'bundle:index', 'bundle:css', 'bundle:static', 'bundle:dependencies', 'bundle:config']);
});

// copy static assets
gulp.task('copy:assets', function() {
  return gulp.src(['app/**/*',
             'css/**/*',
             'images/**/*',
             'index.html',
             'iot-connector.config.js',
             'systemjs.config.js',
             '!app/**/*.ts'], { base : './' })
    .pipe(gulp.dest(dest))
});

gulp.task('copy:config', function() {
  return gulp.src('config/' + (process.env.NODE_ENV == undefined ? 'development' : process.env.NODE_ENV) + '/iot-connector.config.js')
    .pipe(gulp.dest(dest));
})

// compile TypeScript
gulp.task('compile', function () {
  return gulp.src(tscConfig.filesGlob)
    // tsProject.src should work but doesnt
    .pipe(plugins.sourcemaps.init())
    .pipe(tsProject())
    .js
    .pipe(plugins.sourcemaps.write('.'))
    .pipe(gulp.dest(dest + '/app'));
});

gulp.task('connect', function () {
  return plugins.connect.server({
    root: dest,
    port: 5000,
    host: '0.0.0.0'
  })
});

gulp.task('watch', function() {
  // TODO: define assets array
  return gulp.watch(['app/**/*', 'index.html', 'css/*', 'systemjs.config.js'], ['build'])
})

gulp.task('build', ['compile', 'copy:config', 'copy:libs', 'copy:assets', 'fonts']);
gulp.task('default', ['build', 'connect', 'watch']);
