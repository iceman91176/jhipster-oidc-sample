'use strict';

angular.module('jhipsteroidcsampleApp')
    .controller('PasswordController', function ($scope, Auth, Principal) {
        Principal.identity().then(function(account) {
            $scope.account = account;
            $scope.disablePassword=false;
            if(angular.isArray($scope.account.externalAccounts)) {
            	if ($scope.account.externalAccounts.length > 0){
            		$scope.disablePassword=true;
            	}
            }
        });

        $scope.success = null;
        $scope.error = null;
        $scope.doNotMatch = null;
        $scope.changePassword = function () {
            if ($scope.password !== $scope.confirmPassword) {
                $scope.doNotMatch = 'ERROR';
            } else {
                $scope.doNotMatch = null;
                Auth.changePassword($scope.password).then(function () {
                    $scope.error = null;
                    $scope.success = 'OK';
                }).catch(function () {
                    $scope.success = null;
                    $scope.error = 'ERROR';
                });
            }
        };
    });
