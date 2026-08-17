<?php

namespace App {
    class UserModel extends \Hyperf\Database\Model\Model
    {
        protected $table = 'users';

        protected array $casts = ['<caret>'];
    }
}
