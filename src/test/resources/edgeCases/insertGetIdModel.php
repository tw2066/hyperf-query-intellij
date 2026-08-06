<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->insertGetId([
    'email' => 'john@example.com',
    '<caret>' => 1,
]);
