'use strict';

angular.module('jhipsteroidcsampleApp')
    .factory('Password', function ($resource) {
        return $resource('api/account/change_password', {}, {
        });
    });
