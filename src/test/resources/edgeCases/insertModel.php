<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->insert([
    'email' => 'john@example.com',
    '<caret>' => 1,
]);
