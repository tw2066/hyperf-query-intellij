<?php

class User extends \Hyperf\Database\Model\Model {

}

User::query()->first(['id', '<caret>']);
