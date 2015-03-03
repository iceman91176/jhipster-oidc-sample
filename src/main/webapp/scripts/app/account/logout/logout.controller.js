'use strict';

angular.module('jhipsteroidcsampleApp')
    .controller('LogoutController', function (Auth) {
        Auth.logout();
    });
