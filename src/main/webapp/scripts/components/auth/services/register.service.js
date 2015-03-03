'use strict';

angular.module('jhipsteroidcsampleApp')
    .factory('Register', function ($resource) {
        return $resource('api/register', {}, {
        });
    });


