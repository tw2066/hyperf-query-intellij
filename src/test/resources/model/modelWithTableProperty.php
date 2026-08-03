<?php

namespace App {
class DemoButActuallyUsers extends \Hyperf\Database\Model\Model
{
    protected $table = 'users';
}
}

(new \App\DemoButActuallyUsers())->newQuery()->get('<caret>');