module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    handlebars: {
        compile: {
            options: {
                compilerOptions: {
                    knownHelpers: {
                      'eq': true,
                      'ne': true,
                      'lt': true,
                      'gt': true,
                      'lte': true,
                      'gte': true,
                      'and': true,
                      'or': true,
                      'not': true,
                      'switch': true,
                      'case': true,
                      'stripNS': true,
                      'sum': true
                    },
                    knownHelpersOnly: false
                },
                namespace: 'App.Templates',
                processName: function(filePath) {
                    var parts = filePath.split('/');
                    return parts[parts.length - 1];
                },
                processPartialName: function(filePath) {
                    var parts = filePath.split('/');
                    return parts[parts.length - 1];
                },
                partialRegex: /.*/,
                partialsPathRegex: /\/partials\//
            },
            files: {
                './target/temp/itb-templates-json.js': './src/main/resources/templates/js/**/*.hbs'
            }
        }
    },
    concat: {
      options: {
        separator: ';'
      },
      dist: {
        src: ['./src/main/resources/resources/js/itb-upload-json.js', './target/temp/itb-templates-json.js'],
        dest: './target/classes/resources/js/itb-upload-templates-json.js'
      }
    },
    uglify: {
      my_target: {
        files: {
          './target/classes/resources/js/itb-upload-json-min.js': ['./target/classes/resources/js/itb-upload-templates-json.js']
        }
      }
    },
  });
  // Load plugins.
  grunt.loadNpmTasks('grunt-contrib-handlebars');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  // Default task(s).
  grunt.registerTask('default', ['handlebars', 'concat', 'uglify']);
};