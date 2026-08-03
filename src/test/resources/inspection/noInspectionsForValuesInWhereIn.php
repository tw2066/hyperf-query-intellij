<?php

class User extends \Hyperf\Database\Model\Model {
}

User::whereIn('id', [
    'id1',
    'id2',
])->get();
