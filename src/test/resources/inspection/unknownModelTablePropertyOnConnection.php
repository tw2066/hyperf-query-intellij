<?php

namespace App {
    class UserModel extends \Hyperf\Database\Model\Model
    {
        protected ?string $connection = 'goods';

        protected $table = '<warning descr="Unknown table or view">users</warning>';
    }
}
