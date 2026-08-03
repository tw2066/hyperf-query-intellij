<?php

class User extends \Hyperf\Database\Model\Model {

}

User::where([
    ['email', '=', 'test@email.com'],
]);
