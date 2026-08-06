<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->insertOrIgnore([
    'email' => 'john@example.com',
    '<caret>' => 1,
]);
