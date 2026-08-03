<?php

namespace App {
class DemoOneButActuallyUsers extends \Hyperf\Database\Model\Model {
    protected $table = 'users';
}
class Admin extends DemoOneButActuallyUsers {
}
}

(new \App\Admin())->newQuery()->get('<caret>');
