<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->insert([
    ['email' => 'taylor@example.com', 'first_name' => 't'],
    ['email' => 'dayle@example.com', '<caret>' => 1],
]);
