<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->where([
    'id' => 2,
    '<caret>',
]);
