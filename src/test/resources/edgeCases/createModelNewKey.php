<?php

class User extends \Hyperf\Database\Model\Model {

}

User::create([
    'email' => 'email@email.com',
    '<caret>',
]);
